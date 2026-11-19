package com.buildingassessment.common.data;

import java.util.Date;
import java.util.List;

/**
 * What every sensor plugin can do, regardless of type. Sensor-specific SPI
 * interfaces (e.g. {@code IAccelerometer}) extend this, so the host can call
 * {@link #connect()} on whatever {@code ServiceLocator} hands back without
 * casting.
 */
public interface Sensor {

    void connect();
    List<Integer> getSensorIDs();
    int setSensorID(int sensorID);
    Date getSensorTime(int sensorID);
    int durationTimer(int sensorID);
    int messagesNumber(int sensorID); // how many messages is send?

    int batteryLevel(int sensorID);
    int signalPercentage(int sensorID);

    void sendNotification(int reading);

    boolean isCritical(int sensorID);
}
