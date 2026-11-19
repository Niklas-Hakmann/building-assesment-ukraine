package com.buildingassessment.core.server;

import java.nio.file.Path;
import java.util.List;

import com.buildingassessment.common.services.spi.IAccelerometer;
import com.buildingassessment.common.util.ServiceLocator;

/**
 * Entry point and wiring only: find the sensor plugins, connect them to the
 * MQTT broker (MQTT_HOST / MQTT_PORT, default localhost:1883) and start the
 * dashboard's HTTP server (API_PORT, default 8080).
 *
 * <pre>
 *   docker compose up -d
 *   mvn install -DskipTests &amp;&amp; mvn -N exec:exec
 * </pre>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("[core] plugins found: " + ServiceLocator.INSTANCE.pluginModuleNames());

        List<IAccelerometer> accelerometers = ServiceLocator.INSTANCE.locateAll(IAccelerometer.class);
        if (accelerometers.isEmpty()) {
            System.err.println("[core] no IAccelerometer provider found — is sensor-vibration in plugins/?");
        }

        // Bind the HTTP port before connecting to MQTT: once Paho's threads are
        // running they keep the JVM alive, so a "port in use" error after that
        // would leave a half-started process behind instead of exiting.
        int port = Integer.parseInt(System.getenv().getOrDefault("API_PORT", "8080"));
        ApiServer server;
        try {
            // Relative to the working directory, which `mvn -N exec:exec` sets to the repo root.
            server = new ApiServer(port, new SensorReadings(accelerometers), new BuildingRepository(),
                    Path.of("core", "ui", "dist"));
        } catch (java.net.BindException e) {
            System.err.println("[core] port " + port + " is already in use — is the dashboard already running?"
                    + " Stop it, or start this one with API_PORT=<other port>.");
            System.exit(1);
            return;
        }

        for (IAccelerometer sensor : accelerometers) {
            sensor.connect();
            System.out.println("[core] connected " + sensor.getClass().getSimpleName() + " to MQTT");
        }

        server.start();
        System.out.println("[core] dashboard API on http://localhost:" + server.port() + "/api/sensors");
    }
}
