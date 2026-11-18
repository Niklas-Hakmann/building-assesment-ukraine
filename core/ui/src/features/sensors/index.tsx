// Sensor dashboard feature: composes the sensor store and renderer registry into the dashboard page.
import { Fragment, useEffect, useMemo, useRef, useState } from 'react';
import { SegmentedControl, StatTile, StatusBadge, formatAgo } from '../../components';
import { SensorCard, SensorDetails, typeLabel } from '../../renderers';
import { statusOf, useSensorStore } from '../../store';
import type { ConnectionState, SensorReading, SensorStatus } from '../../types';

type Filter = 'all' | 'critical' | 'normal';
type View = 'cards' | 'table';
type Row = { sensor: SensorReading; status: SensorStatus };

const STATUS_ORDER: Record<SensorStatus, number> = { critical: 0, offline: 1, normal: 2 };

/** A sensor that has gone silent needs attention just as much as one reporting critical values. */
const needsAttention = (status: SensorStatus) => status !== 'normal';

const rowKey = (sensor: SensorReading) => `${sensor.type}-${sensor.id}`;

export function SensorDashboard() {
  const { sensors, connection, now } = useSensorStore();
  const [filter, setFilter] = useState<Filter>('all');
  const [view, setView] = useState<View>('table');

  const rows = useMemo<Row[]>(
    () =>
      sensors
        .map((sensor) => ({ sensor, status: statusOf(sensor, now) }))
        .sort((a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status] || a.sensor.id - b.sensor.id),
    [sensors, now],
  );

  const [selected, setSelected] = useState<string | null>(null);
  const selectedRow = rows.find((r) => rowKey(r.sensor) === selected) ?? null;

  const attention = rows.filter((r) => needsAttention(r.status)).length;
  const reporting = rows.filter((r) => r.status !== 'offline').length;
  const visible =
    filter === 'all' ? rows : rows.filter((r) => needsAttention(r.status) === (filter === 'critical'));

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1>Building Assessment Ukraine</h1>
          <p className="page__subtitle">Structural sensor dashboard</p>
        </div>
      </header>

      <section className="tiles tiles--two" aria-label="Summary">
        <StatTile label="Sensors reporting" value={reporting} detail={`of ${rows.length} known`} />
        <StatTile
          label="Critical"
          value={attention}
          tone={attention > 0 ? 'critical' : undefined}
          detail={attention > 0 ? 'Needs inspection' : 'None'}
        />
      </section>

      <div className="toolbar">
        <SegmentedControl<Filter>
          label="Filter by status"
          value={filter}
          onChange={setFilter}
          options={[
            { value: 'all', label: `All (${rows.length})` },
            { value: 'critical', label: `Critical (${attention})` },
            { value: 'normal', label: `Normal (${rows.length - attention})` },
          ]}
        />
        <SegmentedControl<View>
          label="View"
          value={view}
          onChange={setView}
          options={[
            { value: 'table', label: 'Table' },
            { value: 'cards', label: 'Cards' },
          ]}
        />
      </div>

      {rows.length === 0 ? (
        <EmptyState connection={connection} />
      ) : visible.length === 0 ? (
        <p className="empty">No sensors match this filter.</p>
      ) : view === 'cards' ? (
        <section className="grid" aria-label="Sensors">
          {visible.map(({ sensor, status }) => (
            <SensorCard
              key={rowKey(sensor)}
              sensor={sensor}
              status={status}
              now={now}
              onOpen={() => setSelected(rowKey(sensor))}
            />
          ))}
        </section>
      ) : (
        <SensorTable rows={visible} now={now} />
      )}

      {selectedRow && <SensorDialog row={selectedRow} onClose={() => setSelected(null)} />}
    </div>
  );
}

/**
 * Native modal <dialog>: focus trap, Esc to close and the backdrop come from the
 * browser. It looks the sensor up by key on every render, so the numbers keep
 * updating while it is open.
 */
function SensorDialog({ row, onClose }: { row: Row; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    // No close() in a cleanup: StrictMode re-runs effects in dev, and the
    // queued "close" event would then dismiss the freshly reopened dialog.
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  const { sensor, status } = row;
  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby="modal-title"
      onClose={onClose}
      // A click on the backdrop lands on the <dialog> itself, not its content.
      onClick={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className="modal__body">
        <header className="modal__header">
          <div>
            <h2 id="modal-title" className="card__title">
              Sensor {sensor.id}
            </h2>
            <div className="card__subtitle">{typeLabel(sensor.type)}</div>
          </div>
          <StatusBadge status={status} />
          <button type="button" className="modal__close" onClick={onClose} aria-label="Close">
            <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
              <path d="m4 4 8 8M12 4l-8 8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </header>
        <SensorDetails sensor={sensor} />
      </div>
    </dialog>
  );
}

function EmptyState({ connection }: { connection: ConnectionState }) {
  return (
    <div className="empty">
      {connection === 'disconnected' ? (
        <>
          <strong>Cannot reach the backend.</strong>
          <p>
            Start it with <code>mvn -N exec:exec</code> from the repository root.
          </p>
        </>
      ) : (
        <>
          <strong>Waiting for sensor data…</strong>
          <p>
            Is the MQTT broker running? <code>docker compose up -d</code>
          </p>
        </>
      )}
    </div>
  );
}

function SensorTable({ rows, now }: { rows: Row[]; now: number }) {
  const [open, setOpen] = useState<Set<string>>(() => new Set());
  const toggle = (key: string) =>
    setOpen((current) => {
      const next = new Set(current);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Sensor</th>
            <th scope="col">Status</th>
            <th scope="col" className="num col-battery">Battery</th>
            <th scope="col" className="num">Last seen</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ sensor, status }) => {
            const key = rowKey(sensor);
            const isOpen = open.has(key);
            return (
              <Fragment key={key}>
                <tr className={`data-row${isOpen ? ' is-open' : ''}`} onClick={() => toggle(key)}>
                  <th scope="row">
                    <button
                      type="button"
                      className="row-toggle"
                      aria-expanded={isOpen}
                      aria-controls={`details-${key}`}
                      onClick={(event) => {
                        event.stopPropagation();
                        toggle(key);
                      }}
                    >
                      <Chevron />
                      <span>
                        Sensor {sensor.id}
                        <span className="muted">{typeLabel(sensor.type)}</span>
                      </span>
                    </button>
                  </th>
                  <td>
                    <StatusBadge status={status} />
                  </td>
                  <td className="num col-battery">{sensor.batteryLevel}%</td>
                  <td className="num">{formatAgo(now - Date.parse(sensor.sensorTime))}</td>
                </tr>
                {isOpen && (
                  <tr className="detail-row" id={`details-${key}`}>
                    <td colSpan={4}>
                      <SensorDetails sensor={sensor} />
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function Chevron() {
  return (
    <svg className="chevron" width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
      <path d="m6 3.5 4.5 4.5L6 12.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
