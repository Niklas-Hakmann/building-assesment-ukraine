/**
 * The host: entry point, wiring, and the pipeline that drives a message from the
 * broker to the dashboard.
 *
 * <p>Requires {@code common} only. The pipeline modules and sensor plugins are
 * resolved at runtime through {@code ServiceLocator}, so none of them appear
 * here — which is precisely why a plugin JAR can be dropped into
 * {@code plugins/} without this module being rebuilt.
 */
module com.buildingassessment.app {
    requires com.buildingassessment.common;
}
