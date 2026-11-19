// Stores: the live reading per sensor (MQTT, via /api/sensors) and the buildings
// with their installed sensors (database, via /api/buildings).
import { useEffect, useState } from 'react';
import { fetchBuildings, subscribeToSensors } from '../realtime';
import type { Building, ConnectionState, SensorReading, SensorRef, SensorStatus } from '../types';

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

/** A sensor that has gone silent needs attention just as much as one reporting critical values. */
export const needsAttention = (status: SensorStatus) => status !== 'normal';

export const sensorKey = (sensor: { type: string; id: number }) => `${sensor.type}-${sensor.id}`;

const BUILDINGS_REFRESH_MS = 30_000;
const BUILDINGS_RETRY_MS = 5_000;

export interface BuildingStore {
  buildings: Building[] | null;
  error: string | null;
}

/** Buildings from the database: loaded once, refreshed every 30 s, retried sooner after an error. */
export function useBuildingStore(): BuildingStore {
  const [buildings, setBuildings] = useState<Building[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout>;
    async function load() {
      try {
        const response = await fetchBuildings();
        if (stopped) return;
        setBuildings(response.buildings);
        setError(null);
        timer = setTimeout(load, BUILDINGS_REFRESH_MS);
      } catch (e) {
        if (stopped) return;
        setError(e instanceof Error ? e.message : String(e));
        timer = setTimeout(load, BUILDINGS_RETRY_MS);
      }
    }
    load();
    return () => {
      stopped = true;
      clearTimeout(timer);
    };
  }, []);

  return { buildings, error };
}

export interface SensorRow {
  sensor: SensorReading;
  status: SensorStatus;
}

export interface BuildingRow {
  building: Building;
  /** Installed sensors that have reported, worst status first. */
  rows: SensorRow[];
  /** Installed sensors with no reading at all yet. */
  missing: SensorRef[];
  status: SensorStatus;
  /** Sensors needing attention: critical, silent, or never reported. */
  attention: number;
  lastSeen: number | null;
}

const STATUS_ORDER: Record<SensorStatus, number> = { critical: 0, offline: 1, normal: 2 };

export function sortRows(rows: SensorRow[]): SensorRow[] {
  return [...rows].sort((a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status] || a.sensor.id - b.sensor.id);
}

/** Joins each building's installed sensors (database) with their live readings (MQTT). */
export function buildingRows(buildings: Building[], sensors: SensorReading[], now: number): BuildingRow[] {
  const readings = new Map(sensors.map((s) => [sensorKey(s), s]));
  return buildings
    .map((building) => {
      const rows: SensorRow[] = [];
      const missing: SensorRef[] = [];
      for (const ref of building.sensors) {
        const reading = readings.get(sensorKey(ref));
        if (reading) rows.push({ sensor: reading, status: statusOf(reading, now) });
        else missing.push(ref);
      }
      const statuses = rows.map((r) => r.status);
      const status: SensorStatus = statuses.includes('critical')
        ? 'critical'
        : statuses.includes('offline') || missing.length > 0
          ? 'offline'
          : 'normal';
      const times = rows.map((r) => Date.parse(r.sensor.sensorTime));
      return {
        building,
        rows: sortRows(rows),
        missing,
        status,
        attention: rows.filter((r) => needsAttention(r.status)).length + missing.length,
        lastSeen: times.length ? Math.max(...times) : null,
      };
    })
    .sort((a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status] || a.building.id - b.building.id);
}

export function addressOf(building: Building): string {
  return `${building.roadName} ${building.houseNumber}`;
}
