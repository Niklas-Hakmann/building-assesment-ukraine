/**
 * Sensor plugin — the real vibration/strain device.
 *
 * <p>A drop-in plugin: nothing requires this module, and the host never names
 * it. Its JAR goes to {@code plugins/} rather than the boot layer, because
 * {@code sensor-vibration/pom.xml} overrides {@code module.outputDir}.
 *
 * <p>Add a {@code provides} clause for each SPI contract this plugin
 * implements, once those contracts exist in {@code common}:
 * <pre>{@code
 *   provides com.buildingassessment.common.services.spi.YourSpi
 *           with com.buildingassessment.sensor.vibration.YourImpl;
 * }</pre>
 *
 * <p>Export nothing. A plugin that exports a package invites a second plugin to
 * depend on it, and two plugins that know about each other can no longer be
 * removed independently.
 */
module com.buildingassessment.sensor.vibration {
    requires com.buildingassessment.common;
}
