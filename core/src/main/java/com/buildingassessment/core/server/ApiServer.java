package com.buildingassessment.core.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP server for the dashboard, on the JDK's built-in server so {@code core}
 * needs no third-party dependency.
 *
 * <ul>
 *   <li>{@code GET /api/sensors} — latest reading of every sensor as JSON.</li>
 *   <li>Everything else — the built dashboard from {@code core/ui/dist}, when it
 *       exists. During development Vite serves the UI instead and proxies
 *       {@code /api} here (see {@code core/ui/vite.config.ts}).</li>
 * </ul>
 */
public class ApiServer {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "svg", "image/svg+xml",
            "json", "application/json",
            "png", "image/png",
            "ico", "image/x-icon");

    private final SensorReadings readings;
    private final BuildingRepository buildings;
    private final Path staticRoot;
    private final HttpServer server;

    public ApiServer(int port, SensorReadings readings, BuildingRepository buildings, Path staticRoot)
            throws IOException {
        this.readings = readings;
        this.buildings = buildings;
        this.staticRoot = staticRoot.toAbsolutePath().normalize();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/sensors", this::handleSensors);
        server.createContext("/api/buildings", this::handleBuildings);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handleSensors(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String sensors = readings.snapshot().stream()
                .map(SensorSnapshot::toJson)
                .collect(Collectors.joining(","));
        String body = "{\"serverTime\":\"" + Instant.now() + "\",\"sensors\":[" + sensors + "]}";
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        send(exchange, 200, "application/json", body.getBytes(StandardCharsets.UTF_8));
    }

    /** Buildings and the sensors installed in each, from PostgreSQL. 503 if the database is unreachable. */
    private void handleBuildings(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        try {
            String body = "{\"buildings\":["
                    + buildings.findAll().stream()
                            .map(BuildingRepository.Building::toJson)
                            .collect(Collectors.joining(","))
                    + "]}";
            send(exchange, 200, "application/json", body.getBytes(StandardCharsets.UTF_8));
        } catch (java.sql.SQLException e) {
            System.err.println("[api] database query failed: " + e.getMessage());
            String body = "{\"error\":" + Json.string("Database unavailable: " + e.getMessage()) + "}";
            send(exchange, 503, "application/json", body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }
        Path file = staticRoot.resolve(requestPath.substring(1)).normalize();

        // Refuse anything that escapes the static root (e.g. /../../etc/passwd).
        if (!file.startsWith(staticRoot) || !Files.isRegularFile(file)) {
            String message = Files.isDirectory(staticRoot)
                    ? "Not Found"
                    : "Dashboard not built. Run `npm run build` in core/ui, or use `npm run dev`.";
            send(exchange, 404, "text/plain; charset=utf-8", message.getBytes(StandardCharsets.UTF_8));
            return;
        }
        String name = file.getFileName().toString();
        String extension = name.substring(name.lastIndexOf('.') + 1);
        send(exchange, 200, CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"), Files.readAllBytes(file));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
