// Sensor store: holds the current reading per sensor plus connection state.
import { useEffect, useState } from 'react';
import { subscribeToSensors } from '../realtime';
import type { ConnectionState, SensorReading, SensorStatus } from '../types';

/** The simulator publishes every 10 s; three missed messages means offline. */
export const OFFLINE_AFTER_MS = 30_000;

export interface SensorStore {
  sensors: SensorReading[];
  connection: ConnectionState;
  /** Browser time of the last successful fetch. */
  lastUpdate: number | null;
  /** Current time in the server's clock, ticking every second. */
  now: number;
}

export function useSensorStore(): SensorStore {
  const [sensors, setSensors] = useState<SensorReading[]>([]);
  const [connection, setConnection] = useState<ConnectionState>('connecting');
  const [lastUpdate, setLastUpdate] = useState<number | null>(null);
  // serverTime - browser time, so "seconds ago" is right even if the clocks differ.
  const [clockOffset, setClockOffset] = useState(0);
  const [tick, setTick] = useState(Date.now());

  useEffect(
    () =>
      subscribeToSensors(
        (response) => {
          setSensors(response.sensors);
          setClockOffset(Date.parse(response.serverTime) - Date.now());
          setLastUpdate(Date.now());
          setConnection('live');
        },
        () => setConnection('disconnected'),
      ),
    [],
  );

  useEffect(() => {
    const id = setInterval(() => setTick(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  return { sensors, connection, lastUpdate, now: tick + clockOffset };
}

export function statusOf(sensor: SensorReading, now: number): SensorStatus {
  if (now - Date.parse(sensor.sensorTime) > OFFLINE_AFTER_MS) return 'offline';
  return sensor.critical ? 'critical' : 'normal';
}

export function peakOf(sensor: SensorReading): number {
  const { x, y, z } = sensor.maxVibrations;
  return Math.max(x, y, z);
}
