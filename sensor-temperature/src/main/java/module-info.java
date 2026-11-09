/**
 * Sensor plugin — simulated temperature, and the proof that a new sensor type
 * costs one module.
 *
 * <p>Meant to stay a copy of {@code sensor-vibration} with a different type
 * name, payload and sensor ids — nothing else. If adding a sensor type ever
 * requires editing a module outside its own directory, the plugin boundary has
 * leaked.
 *
 * <p>Its JAR goes to {@code plugins/} rather than the boot layer, because
 * {@code sensor-temperature/pom.xml} overrides {@code module.outputDir}.
 */
module com.buildingassessment.sensor.temperature {
    requires com.buildingassessment.common;
}
