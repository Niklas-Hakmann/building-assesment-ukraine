package com.buildingassessment.core.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.buildingassessment.common.services.spi.IAccelerometer;
import com.buildingassessment.core.server.SensorSnapshot.Axes;

/**
 * Collects the latest reading of every sensor from every loaded plugin. This is
 * the one place that knows how to turn a plugin interface into a
 * {@link SensorSnapshot}; a new sensor type adds a loop here.
 */
public class SensorReadings {

    private final List<IAccelerometer> accelerometers;

    public SensorReadings(List<IAccelerometer> accelerometers) {
        this.accelerometers = List.copyOf(accelerometers);
    }

    public List<SensorSnapshot> snapshot() {
        List<SensorSnapshot> result = new ArrayList<>();
        for (IAccelerometer sensor : accelerometers) {
            for (int id : sensor.getSensorIDs()) {
                result.add(new SensorSnapshot(
                        "accelerometer",
                        id,
                        sensor.getSensorTime(id).toInstant(),
                        sensor.durationTimer(id),
                        sensor.messagesNumber(id),
                        sensor.batteryLevel(id),
                        sensor.isCritical(id),
                        new Axes(sensor.getDominantFrequencyX(id),
                                sensor.getDominantFrequencyY(id),
                                sensor.getDominantFrequencyZ(id)),
                        new Axes(sensor.maxVibrationsX(id),
                                sensor.maxVibrationsY(id),
                                sensor.maxVibrationsZ(id)),
                        new Axes(sensor.totalVibrationsX(id),
                                sensor.totalVibrationsY(id),
                                sensor.totalVibrationsZ(id))));
            }
        }
        result.sort(Comparator.comparing(SensorSnapshot::type).thenComparingInt(SensorSnapshot::id));
        return result;
    }
}
