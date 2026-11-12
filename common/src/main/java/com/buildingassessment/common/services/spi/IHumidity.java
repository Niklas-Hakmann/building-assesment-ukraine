package com.buildingassessment.common.services.spi;

public interface IHumidity {
    public int getHumidity(int sensorID);
    public int getTemperature(int sensorID);
}
