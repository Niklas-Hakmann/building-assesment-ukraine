# Simulators

Placeholder for a generic mock sensor publisher that publishes fake sensor readings
to the MQTT broker, using the same topic/payload convention as the real sensor
(`sensors/{sensorType}/{sensorId}`). Configured by a list of mock sensor definitions
(id, type, value-generation strategy) rather than one script per sensor, so adding a
new mock sensor is a config change, not new code.

Language/runtime for this component is not yet decided. See
[ADR-0001](../docs/architecture/ADR-0001-architecture-overview.md).
