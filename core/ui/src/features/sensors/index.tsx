// Sensor list feature: filter, table/cards view and the detail popup for a set of sensors.
// Used inside a building's page.
import { Fragment, useEffect, useRef, useState } from 'react';
import { SegmentedControl, StatusBadge, formatAgo } from '../../components';
import { SensorCard, SensorDetails, typeLabel } from '../../renderers';
import { needsAttention, sensorKey, type SensorRow } from '../../store';

type Filter = 'all' | 'critical' | 'normal';
export type View = 'cards' | 'table';

export function SensorList({ rows, now }: { rows: SensorRow[]; now: number }) {
  const [filter, setFilter] = useState<Filter>('all');
  const [view, setView] = useState<View>('table');
  const [selected, setSelected] = useState<string | null>(null);
  const selectedRow = rows.find((r) => sensorKey(r.sensor) === selected) ?? null;

  const attention = rows.filter((r) => needsAttention(r.status)).length;
  const visible = filter === 'all' ? rows : rows.filter((r) => needsAttention(r.status) === (filter === 'critical'));

  return (
    <>
      <div className="toolbar">
        <SegmentedControl<Filter>
          label="Filter sensors by status"
          value={filter}
          onChange={setFilter}
          options={[
            { value: 'all', label: `All (${rows.length})` },
            { value: 'critical', label: `Critical (${attention})` },
            { value: 'normal', label: `Normal (${rows.length - attention})` },
          ]}
        />
        <ViewToggle value={view} onChange={setView} />
      </div>

      {visible.length === 0 ? (
        <p className="empty">No sensors match this filter.</p>
      ) : view === 'cards' ? (
        <section className="grid" aria-label="Sensors">
          {visible.map(({ sensor, status }) => (
            <SensorCard
              key={sensorKey(sensor)}
              sensor={sensor}
              status={status}
              now={now}
              onOpen={() => setSelected(sensorKey(sensor))}
            />
          ))}
        </section>
      ) : (
        <SensorTable rows={visible} now={now} />
      )}

      {selectedRow && <SensorDialog row={selectedRow} onClose={() => setSelected(null)} />}
    </>
  );
}

export function ViewToggle({ value, onChange }: { value: View; onChange: (view: View) => void }) {
  return (
    <SegmentedControl<View>
      label="View"
      value={value}
      onChange={onChange}
      options={[
        { value: 'table', label: 'Table' },
        { value: 'cards', label: 'Cards' },
      ]}
    />
  );
}

/**
 * Native modal <dialog>: focus trap, Esc to close and the backdrop come from the
 * browser. It looks the sensor up by key on every render, so the numbers keep
 * updating while it is open.
 */
function SensorDialog({ row, onClose }: { row: SensorRow; onClose: () => void }) {
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

function SensorTable({ rows, now }: { rows: SensorRow[]; now: number }) {
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
            const key = sensorKey(sensor);
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

export function Chevron() {
  return (
    <svg className="chevron" width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
      <path d="m6 3.5 4.5 4.5L6 12.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
