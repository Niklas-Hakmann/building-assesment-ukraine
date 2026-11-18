package com.buildingassessment.common.services.spi;

public interface IAccelerometer {

    public double getDominantFrequencyX(int sensorID);
    public double getDominantFrequencyY(int sensorID);
    public double getDominantFrequencyZ(int sensorID);
    public int totalVibrationsX(int sensorID);
    public int totalVibrationsY(int sensorID);
    public int totalVibrationsZ(int sensorID);
    public int maxVibrationsX(int sensorID);
    public int maxVibrationsY(int sensorID);
    public int maxVibrationsZ(int sensorID);

}
