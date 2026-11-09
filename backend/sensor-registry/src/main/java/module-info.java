/**
 * Tracks which sensors exist, independently of their readings.
 */
module com.buildingassessment.sensorregistry {
    requires com.buildingassessment.common;

    provides com.buildingassessment.common.services.SensorCatalog
            with com.buildingassessment.sensorregistry.internal.InMemorySensorCatalog;
}
