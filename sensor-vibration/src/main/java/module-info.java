import com.buildingassessment.sensor.vibration.VibrationSensor;

module sensorVibration {
    requires common;
    requires org.eclipse.paho.client.mqttv3;
    requires org.json;
    provides IAccelerationSensor with VibrationSensor;
}
