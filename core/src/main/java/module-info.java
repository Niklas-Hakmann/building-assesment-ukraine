/**
 * The host: entry point, wiring, and whatever drives a message from the broker
 * to the dashboard.
 *
 * <p>Requires {@code common} only, and that is the rule worth keeping. Sensor
 * plugins are meant to be resolved at runtime through {@code ServiceLocator}, so
 * none of them may appear here — a {@code requires} on a sensor module is what
 * would stop a plugin JAR from being droppable into {@code plugins/} without
 * rebuilding this module.
 *
 * <p>The root {@code pom.xml} runs
 * {@code com.buildingassessment.core.server.Main}, which does not exist yet.
 *
 * <p>Add {@code exports com.buildingassessment.core.server;} once that package
 * has a class, and a {@code provides} clause for each "choose one" service role
 * this module implements. No {@code uses} clauses belong here: this module never
 * calls {@code ServiceLoader} itself — {@code ServiceLocator} does, from
 * {@code common}, which is where the matching {@code uses} declarations live.
 */
module com.buildingassessment.core {
    requires com.buildingassessment.common;
}
