// Shared TypeScript types mirroring the JSON from GET /api/sensors
// (core/server/SensorSnapshot.java).

export interface Axes {
  x: number;
  y: number;
  z: number;
}

export interface SensorReading {
  type: string;
  id: number;
  /** ISO-8601, when the sensor took the reading. */
  sensorTime: string;
  durationTimer: number;
  messagesNumber: number;
  batteryLevel: number;
  critical: boolean;
  /** Hz */
  dominantFrequency: Axes;
  /** Peak acceleration in the last interval, mg (thousandths of g). */
  maxVibrations: Axes;
  /** Number of vibration events in the last interval. */
  totalVibrations: Axes;
}

export interface SensorsResponse {
  serverTime: string;
  sensors: SensorReading[];
}

export type SensorStatus = 'critical' | 'offline' | 'normal';

export type ConnectionState = 'connecting' | 'live' | 'disconnected';
