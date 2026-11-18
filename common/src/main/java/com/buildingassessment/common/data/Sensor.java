package com.buildingassessment.common.data;

import java.util.Date;
import java.util.List;

public abstract class Sensor {

    public abstract void connect();
    public abstract List<Integer> getSensorIDs();
    public abstract int setSensorID(int sensorID);
    public abstract Date getSensorTime(int sensorID);
    public abstract int durationTimer(int sensorID);
    public abstract int messagesNumber(int sensorID); // how many messages is send?

    public abstract int batteryLevel(int sensorID);
    public abstract int signalPercentage(int sensorID);

    public abstract void sendNotification(int reading);

    public abstract boolean isCritical(int sensorID);
}
