package com.buildingassessment.common.services.spi;

import java.util.Date;

public interface ISensorGeneral {
    public int getSensorID();
    public int setSensorID(int sensorID);
    public Date getSensorTime();
    public int durationTimer();
    public int messagesNumber(); // how many messages is send?

    public int batteryLevel(int sensorID);
    public int signalPercentage(int sensorID);

    public void sendNotification(int reading);

    public boolean isCritical(int sensorID );
}
