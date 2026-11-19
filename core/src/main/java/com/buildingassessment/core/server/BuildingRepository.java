package com.buildingassessment.core.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads buildings (the Address table) and the sensors installed in each from
 * PostgreSQL. Connection settings come from DB_URL / DB_USER / DB_PASSWORD and
 * default to the database in docker-compose.yml.
 *
 * <p>Opens a connection per call: the dashboard asks rarely, and it means the
 * API recovers by itself if the database was down when the host started.
 */
public class BuildingRepository {

    public record SensorRef(int id, String type) {
        String toJson() {
            return "{\"id\":" + id + ",\"type\":" + Json.string(type) + "}";
        }
    }

    public record Building(
            int id,
            String roadName,
            String houseNumber,
            String zip,
            String city,
            int floors,
            List<SensorRef> sensors) {

        String toJson() {
            return "{"
                    + "\"id\":" + id + ","
                    + "\"roadName\":" + Json.string(roadName) + ","
                    + "\"houseNumber\":" + Json.string(houseNumber) + ","
                    + "\"zip\":" + Json.string(zip) + ","
                    + "\"city\":" + Json.string(city) + ","
                    + "\"floors\":" + floors + ","
                    + "\"sensors\":[" + sensors.stream().map(SensorRef::toJson).collect(Collectors.joining(",")) + "]"
                    + "}";
        }
    }

    private static final String QUERY = """
            SELECT a.address_id, a.road_name, a.house_number, a.zip, z.city_name,
                   a.number_of_floors, s.sensor_id, s.type
            FROM address a
            JOIN zip z ON z.zip = a.zip
            LEFT JOIN sensor s ON s.address = a.address_id
            ORDER BY a.address_id, s.sensor_id
            """;

    private final String url;
    private final String user;
    private final String password;

    public BuildingRepository() {
        Map<String, String> env = System.getenv();
        this.url = env.getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/building_assessment");
        this.user = env.getOrDefault("DB_USER", "building");
        this.password = env.getOrDefault("DB_PASSWORD", "building");
    }

    public List<Building> findAll() throws SQLException {
        Map<Integer, Building> buildings = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(QUERY)) {
            while (rs.next()) {
                int id = rs.getInt("address_id");
                Building building = buildings.computeIfAbsent(id, key -> {
                    try {
                        return new Building(key,
                                rs.getString("road_name"),
                                rs.getString("house_number"),
                                rs.getString("zip"),
                                rs.getString("city_name"),
                                rs.getInt("number_of_floors"),
                                new ArrayList<>());
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                });
                int sensorId = rs.getInt("sensor_id");
                if (!rs.wasNull()) {
                    building.sensors().add(new SensorRef(sensorId, rs.getString("type")));
                }
            }
        }
        return List.copyOf(buildings.values());
    }

    /** For log lines: the URL without credentials (there are none in it, but keep it that way). */
    public String describe() {
        return url;
    }
}
