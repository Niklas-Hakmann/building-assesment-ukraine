package com.buildingassessment.core.server;

import java.time.Instant;

/**
 * The latest reading of one sensor, as the dashboard receives it. Plain data,
 * decoupled from the plugin interfaces so the JSON shape does not change when a
 * plugin's internals do.
 */
public record SensorSnapshot(
        String type,
        int id,
        Instant sensorTime,
        int durationTimer,
        int messagesNumber,
        int batteryLevel,
        boolean critical,
        Axes dominantFrequency,
        Axes maxVibrations,
        Axes totalVibrations) {

    public record Axes(double x, double y, double z) {
        String toJson() {
            return "{\"x\":" + number(x) + ",\"y\":" + number(y) + ",\"z\":" + number(z) + "}";
        }
    }

    /**
     * Hand-written rather than a JSON library: {@code core} may only require
     * {@code common}, and every field here is a number, boolean or a fixed string.
     */
    public String toJson() {
        return "{"
                + "\"type\":\"" + type + "\","
                + "\"id\":" + id + ","
                + "\"sensorTime\":\"" + sensorTime + "\","
                + "\"durationTimer\":" + durationTimer + ","
                + "\"messagesNumber\":" + messagesNumber + ","
                + "\"batteryLevel\":" + batteryLevel + ","
                + "\"critical\":" + critical + ","
                + "\"dominantFrequency\":" + dominantFrequency.toJson() + ","
                + "\"maxVibrations\":" + maxVibrations.toJson() + ","
                + "\"totalVibrations\":" + totalVibrations.toJson()
                + "}";
    }

    /** Locale-independent (no "2,39"), and JSON has no NaN/Infinity. */
    private static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "null";
        }
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
