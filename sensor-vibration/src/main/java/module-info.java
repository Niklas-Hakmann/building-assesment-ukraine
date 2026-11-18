import com.buildingassessment.sensor.vibration.AccelerationSensor;
import com.buildingassessment.common.services.spi.IAccelerometer;

module sensorVibration {
    requires common;
    requires org.eclipse.paho.client.mqttv3;
    requires org.json;
    provides IAccelerometer with AccelerationSensor;
}
