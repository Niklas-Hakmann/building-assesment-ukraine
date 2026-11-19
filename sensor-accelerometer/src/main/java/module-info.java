import com.buildingassessment.common.services.spi.IAccelerometer;
import com.buildingassessment.sensor.accelerometer.AccelerometerSensor;

module sensorAccelerometer {
    requires common;
    requires org.eclipse.paho.client.mqttv3;
    requires org.json;
    provides IAccelerometer with AccelerometerSensor;
}
