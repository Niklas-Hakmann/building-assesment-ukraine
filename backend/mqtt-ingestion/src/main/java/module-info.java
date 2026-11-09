/**
 * Subscribes to the broker and forwards raw messages for decoding.
 *
 * <p>Owns the topic convention {@code sensors/{sensorType}/{sensorId}} and
 * nothing else: it does not know which decoders exist, and never looks inside a
 * payload.
 */
module com.buildingassessment.mqttingestion {
    requires com.buildingassessment.common;

    exports com.buildingassessment.mqttingestion;

    provides com.buildingassessment.common.services.MessageSource
            with com.buildingassessment.mqttingestion.internal.MqttMessageSource;
}
