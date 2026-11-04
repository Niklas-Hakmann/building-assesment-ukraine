/**
 * Decodes raw MQTT payloads into the common SensorReading type. One decoder per
 * sensor type; only vibration/strain is in scope right now. Adding a new sensor
 * type means adding a new decoder here, without changing any other module.
 */
package com.buildingassessment.sensordecoding;
