import com.buildingassessment.sensor.vibration.VibrationSensor;
import com.buildingassessment.common.services.spi.IVibrationSensor;

module sensorVibration {
    requires common;
    requires org.eclipse.paho.client.mqttv3;
    requires org.json;
    provides IVibrationSensor with VibrationSensor;
}
