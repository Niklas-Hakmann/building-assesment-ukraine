package com.buildingassessment.sensor.accelerometer;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import com.buildingassessment.common.services.spi.IAccelerometer;

public class AccelerometerSensor implements IAccelerometer, MqttCallbackExtended {

    private static final String TOPIC_FILTER = "sensors/accelerometer/#";

    private MqttClient mqttClient;
    private final Map<Integer, Reading> readings = new ConcurrentHashMap<>();

    private record Reading(
            Date sensorTime,
            int durationTimer,
            int messagesNumber,
            int batteryLevel,
            double dominantFrequencyX,
            double dominantFrequencyY,
            double dominantFrequencyZ,
            int totalVibrationsX,
            int totalVibrationsY,
            int totalVibrationsZ,
            int maxVibrationsX,
            int maxVibrationsY,
            int maxVibrationsZ) {
    }

    /**
     * Called on the first connect and after every automatic reconnect. With a
     * clean session the broker forgets our subscription when the connection
     * drops, so it has to be re-established here, not only in {@link #connect()}.
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            mqttClient.subscribe(TOPIC_FILTER);
        } catch (MqttException e) {
            System.err.printf("[accelerometer] could not subscribe to %s on %s: %s%n",
                    TOPIC_FILTER, serverURI, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[accelerometer] MQTT connection lost, reconnecting: " + cause.getMessage());
    }

    /**
     * Must not throw: Paho closes the connection if this callback throws, so one
     * malformed message would otherwise cut off every sensor.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            JSONObject payload = new JSONObject(new String(message.getPayload(), StandardCharsets.UTF_8));

            int sensorId = payload.getInt("sensorId");
            Date sensorTime = Date.from(OffsetDateTime.parse(payload.getString("sensorTime")).toInstant());
            int durationTimer = payload.getInt("durationTimer");
            int messagesNumber = payload.getInt("messagesNumber");
            int batteryLevel = payload.getInt("batteryLevel");

            JSONObject dominantFrequency = payload.getJSONObject("dominantFrequency");
            JSONObject totalVibrations = payload.getJSONObject("totalVibrations");
            JSONObject maxVibrations = payload.getJSONObject("maxVibrations");

            readings.put(sensorId, new Reading(
                    sensorTime, durationTimer, messagesNumber, batteryLevel,
                    dominantFrequency.getDouble("x"),
                    dominantFrequency.getDouble("y"),
                    dominantFrequency.getDouble("z"),
                    totalVibrations.getInt("x"),
                    totalVibrations.getInt("y"),
                    totalVibrations.getInt("z"),
                    maxVibrations.getInt("x"),
                    maxVibrations.getInt("y"),
                    maxVibrations.getInt("z")));
        } catch (RuntimeException e) { // JSONException, DateTimeParseException, ...
            System.err.printf("[accelerometer] ignoring malformed message on %s: %s%n", topic, e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    @Override
    public void connect() {
        String host = System.getenv().getOrDefault("MQTT_HOST", "localhost");
        String port = System.getenv().getOrDefault("MQTT_PORT", "1883");
        String brokerUri = "tcp://" + host + ":" + port;

        try {
            mqttClient = new MqttClient(brokerUri, MqttClient.generateClientId(), new MemoryPersistence());
            mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            // Subscribing happens in connectComplete, which also runs after reconnects.
            mqttClient.connect(options);
        } catch (MqttException e) {
            throw new RuntimeException("Failed to connect to MQTT broker at " + brokerUri, e);
        }
    }

    /** The latest reading for a sensor, or a clear error if none has arrived yet. */
    private Reading reading(int sensorID) {
        Reading reading = readings.get(sensorID);
        if (reading == null) {
            throw new IllegalArgumentException("No reading received yet for accelerometer sensor " + sensorID);
        }
        return reading;
    }

    @Override
    public List<Integer> getSensorIDs() {
        return new ArrayList<>(readings.keySet());
    }

    @Override
    public int setSensorID(int sensorID) {
        return 0;
    }

    @Override
    public Date getSensorTime(int sensorID) {
        return reading(sensorID).sensorTime;
    }

    @Override
    public int durationTimer(int sensorID) {
        return reading(sensorID).durationTimer;
    }

    @Override
    public int messagesNumber(int sensorID) {
        return reading(sensorID).messagesNumber;
    }

    @Override
    public int batteryLevel(int sensorID) {
        return reading(sensorID).batteryLevel;
    }

    @Override
    public int signalPercentage(int sensorID) {
        return 0;
    }

    @Override
    public void sendNotification(int reading) {
        System.out.println("dette er en notifikation");
    }

    @Override
    public double getDominantFrequencyX(int sensorID) {
        return reading(sensorID).dominantFrequencyX;
    }

    @Override
    public double getDominantFrequencyY(int sensorID) {
        return reading(sensorID).dominantFrequencyY;
    }

    @Override
    public double getDominantFrequencyZ(int sensorID) {
        return reading(sensorID).dominantFrequencyZ;
    }

    @Override
    public int totalVibrationsX(int sensorID) {
        return reading(sensorID).totalVibrationsX;
    }

    @Override
    public int totalVibrationsY(int sensorID) {
        return reading(sensorID).totalVibrationsY;
    }

    @Override
    public int totalVibrationsZ(int sensorID) {
        return reading(sensorID).totalVibrationsZ;
    }

    @Override
    public int maxVibrationsX(int sensorID) {
        return reading(sensorID).maxVibrationsX;
    }

    @Override
    public int maxVibrationsY(int sensorID) {
        return reading(sensorID).maxVibrationsY;
    }

    @Override
    public int maxVibrationsZ(int sensorID) {
        return reading(sensorID).maxVibrationsZ;
    }

    @Override
    public boolean isCritical(int sensorID) {
        return getDominantFrequencyX(sensorID) > 4;
    }
}
