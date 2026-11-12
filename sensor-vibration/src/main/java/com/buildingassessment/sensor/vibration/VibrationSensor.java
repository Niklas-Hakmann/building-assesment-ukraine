package com.buildingassessment.sensor.vibration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import com.buildingassessment.common.data.Sensor;
import com.buildingassessment.common.services.spi.IVibrationSensor;

public class VibrationSensor extends Sensor implements IVibrationSensor, MqttCallback {

    private static final String VIBRATION_TOPIC_FILTER = "sensors/vibration/#";

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

    @Override
    public void connectionLost(Throwable cause) {
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        JSONObject payload = new JSONObject(new String(message.getPayload()));

        int sensorId = payload.getInt("sensorId");
        Date sensorTime = Date.from(OffsetDateTime.parse(payload.getString("sensorTime")).toInstant());
        int durationTimer = payload.getInt("durationTimer");
        int messagesNumber = payload.getInt("messagesNumber");
        int batteryLevel = payload.getInt("batteryLevel");

        JSONObject dominantFrequency = payload.getJSONObject("dominantFrequency");
        double dominantFrequencyX = dominantFrequency.getDouble("x");
        double dominantFrequencyY = dominantFrequency.getDouble("y");
        double dominantFrequencyZ = dominantFrequency.getDouble("z");

        JSONObject totalVibrations = payload.getJSONObject("totalVibrations");
        int totalVibrationsX = totalVibrations.getInt("x");
        int totalVibrationsY = totalVibrations.getInt("y");
        int totalVibrationsZ = totalVibrations.getInt("z");

        JSONObject maxVibrations = payload.getJSONObject("maxVibrations");
        int maxVibrationsX = maxVibrations.getInt("x");
        int maxVibrationsY = maxVibrations.getInt("y");
        int maxVibrationsZ = maxVibrations.getInt("z");

        readings.put(sensorId, new Reading(
                sensorTime, durationTimer, messagesNumber, batteryLevel,
                dominantFrequencyX, dominantFrequencyY, dominantFrequencyZ,
                totalVibrationsX, totalVibrationsY, totalVibrationsZ,
                maxVibrationsX, maxVibrationsY, maxVibrationsZ));
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
            mqttClient.connect(options);
            mqttClient.subscribe(VIBRATION_TOPIC_FILTER);
        } catch (MqttException e) {
            throw new RuntimeException("Failed to connect to MQTT broker at " + brokerUri, e);
        }
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
        return readings.get(sensorID).sensorTime;
    }

    @Override
    public int durationTimer(int sensorID) {
        return readings.get(sensorID).durationTimer;
    }

    @Override
    public int messagesNumber(int sensorID) {
        return readings.get(sensorID).messagesNumber;
    }

    @Override
    public int batteryLevel(int sensorID) {
        return readings.get(sensorID).batteryLevel;
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
        return readings.get(sensorID).dominantFrequencyX;
    }

    @Override
    public double getDominantFrequencyY(int sensorID) {
        return readings.get(sensorID).dominantFrequencyY;
    }

    @Override
    public double getDominantFrequencyZ(int sensorID) {
        return readings.get(sensorID).dominantFrequencyZ;
    }

    @Override
    public int totalVibrationsX(int sensorID) {
        return readings.get(sensorID).totalVibrationsX;
    }

    @Override
    public int totalVibrationsY(int sensorID) {
        return readings.get(sensorID).totalVibrationsY;
    }

    @Override
    public int totalVibrationsZ(int sensorID) {
        return readings.get(sensorID).totalVibrationsZ;
    }

    @Override
    public int maxVibrationsX(int sensorID) {
        return readings.get(sensorID).maxVibrationsX;
    }

    @Override
    public int maxVibrationsY(int sensorID) {
        return readings.get(sensorID).maxVibrationsY;
    }

    @Override
    public int maxVibrationsZ(int sensorID) {
        return readings.get(sensorID).maxVibrationsZ;
    }

    @Override
    public boolean isCritical(int sensorID) {
        if(getDominantFrequencyX(sensorID) > 4){
            return true;
        }
        return false;
    }
}
