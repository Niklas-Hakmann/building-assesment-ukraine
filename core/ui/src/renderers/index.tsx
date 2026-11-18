// Sensor renderer registry: maps a sensor type to how its measurements are shown.
// A new sensor type = a new details component + one line in DETAIL_RENDERERS.
import type { ComponentType } from 'react';
import { StatusBadge, formatAgo, formatNumber } from '../components';
import type { SensorReading, SensorStatus } from '../types';

const TYPE_LABEL: Record<string, string> = {
  accelerometer: 'Accelerometer',
};

export function typeLabel(type: string): string {
  return TYPE_LABEL[type] ?? type;
}

/** Compact summary card; the measurements open in a popup on click. */
export function SensorCard({
  sensor,
  status,
  now,
  onOpen,
}: {
  sensor: SensorReading;
  status: SensorStatus;
  now: number;
  onOpen: () => void;
}) {
  return (
    <button type="button" className={`card card--${status}`} onClick={onOpen} aria-haspopup="dialog">
      <span className="card__header">
        <span>
          <span className="card__title">Sensor {sensor.id}</span>
          <span className="card__subtitle">{typeLabel(sensor.type)}</span>
        </span>
        <StatusBadge status={status} />
      </span>
      <span className="card__footer">
        <span title="Battery level">
          <BatteryIcon level={sensor.batteryLevel} /> {sensor.batteryLevel}%
        </span>
        <time dateTime={sensor.sensorTime} title={new Date(sensor.sensorTime).toLocaleString()}>
          {formatAgo(now - Date.parse(sensor.sensorTime))}
        </time>
      </span>
    </button>
  );
}

function AccelerometerAxes({ sensor }: { sensor: SensorReading }) {
  const { dominantFrequency: f, maxVibrations: peak, totalVibrations: events } = sensor;
  const rows = [
    { label: 'Frequency', unit: 'Hz', values: f, digits: 2 },
    { label: 'Peak', unit: 'mg', values: peak, digits: 0 },
    { label: 'Events', unit: '', values: events, digits: 0 },
  ];
  return (
    <table className="axes">
      <thead>
        <tr>
          <th scope="col"><span className="visually-hidden">Measurement</span></th>
          <th scope="col">X</th>
          <th scope="col">Y</th>
          <th scope="col">Z</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.label}>
            <th scope="row">
              {row.label}
              {row.unit && <span className="unit"> {row.unit}</span>}
            </th>
            <td>{formatNumber(row.values.x, row.digits)}</td>
            <td>{formatNumber(row.values.y, row.digits)}</td>
            <td>{formatNumber(row.values.z, row.digits)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

const DETAIL_RENDERERS: Record<string, ComponentType<{ sensor: SensorReading }>> = {
  accelerometer: AccelerometerAxes,
};

/** The full measurements of one sensor — shown in the table's expanded row and in the card popup. */
export function SensorDetails({ sensor }: { sensor: SensorReading }) {
  const Measurements = DETAIL_RENDERERS[sensor.type];
  return (
    <div className="detail">
      {Measurements ? (
        <Measurements sensor={sensor} />
      ) : (
        <p className="card__empty">No details for this sensor type yet.</p>
      )}
      <dl className="detail__meta">
        <div>
          <dt>Battery</dt>
          <dd>{sensor.batteryLevel}%</dd>
        </div>
        <div>
          <dt>Messages received</dt>
          <dd>{sensor.messagesNumber}</dd>
        </div>
        <div>
          <dt>Reading taken</dt>
          <dd>{new Date(sensor.sensorTime).toLocaleString()}</dd>
        </div>
      </dl>
    </div>
  );
}

function BatteryIcon({ level }: { level: number }) {
  const width = Math.max(1, Math.min(10, Math.round(level / 10)));
  return (
    <svg width="18" height="10" viewBox="0 0 18 10" aria-hidden="true" className="battery">
      <rect x="0.5" y="0.5" width="14" height="9" rx="2" fill="none" stroke="currentColor" />
      <rect x="15.5" y="3" width="1.5" height="4" rx="0.5" fill="currentColor" />
      <rect x="2.5" y="2.5" width={width} height="5" rx="1" fill="currentColor" />
    </svg>
  );
}
