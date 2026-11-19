// Buildings feature: the overview of all buildings, and one building's page with its sensors.
import { useMemo, useState } from 'react';
import { SegmentedControl, StatTile, StatusBadge, formatAgo } from '../../components';
import { addressOf, buildingRows, needsAttention, type BuildingRow } from '../../store';
import type { Building, ConnectionState, SensorReading } from '../../types';
import { Chevron, SensorList, ViewToggle, type View } from '../sensors';

type Filter = 'all' | 'critical' | 'normal';

export const buildingHref = (building: Building) => `#/buildings/${building.id}`;

interface Props {
  buildings: Building[];
  sensors: SensorReading[];
  now: number;
}

export function BuildingsOverview({ buildings, sensors, now }: Props) {
  const [filter, setFilter] = useState<Filter>('all');
  const [view, setView] = useState<View>('table');
  const rows = useMemo(() => buildingRows(buildings, sensors, now), [buildings, sensors, now]);

  const attention = rows.filter((r) => needsAttention(r.status)).length;
  const installed = rows.reduce((sum, r) => sum + r.building.sensors.length, 0);
  const reporting = rows.reduce((sum, r) => sum + r.rows.filter((s) => s.status !== 'offline').length, 0);
  const visible = filter === 'all' ? rows : rows.filter((r) => needsAttention(r.status) === (filter === 'critical'));

  return (
    <>
      <section className="tiles" aria-label="Summary">
        <StatTile label="Buildings" value={rows.length} detail="Monitored" />
        <StatTile
          label="Needs inspection"
          value={attention}
          tone={attention > 0 ? 'critical' : undefined}
          detail={attention > 0 ? `of ${rows.length} buildings` : 'None'}
        />
        <StatTile label="Sensors reporting" value={reporting} detail={`of ${installed} installed`} />
      </section>

      <div className="toolbar">
        <SegmentedControl<Filter>
          label="Filter buildings by status"
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

      {rows.length === 0 ? (
        <div className="empty">
          <strong>No buildings in the database.</strong>
          <p>
            Add rows to the <code>address</code> table, or reset it from <code>init.sql</code>.
          </p>
        </div>
      ) : visible.length === 0 ? (
        <p className="empty">No buildings match this filter.</p>
      ) : view === 'cards' ? (
        <section className="grid" aria-label="Buildings">
          {visible.map((row) => (
            <BuildingCard key={row.building.id} row={row} now={now} />
          ))}
        </section>
      ) : (
        <BuildingTable rows={visible} now={now} />
      )}
    </>
  );
}

function sensorSummary(row: BuildingRow): string {
  const total = row.building.sensors.length;
  const base = `${total} sensor${total === 1 ? '' : 's'}`;
  return row.attention > 0 ? `${base} · ${row.attention} need${row.attention === 1 ? 's' : ''} attention` : base;
}

function BuildingTable({ rows, now }: { rows: BuildingRow[]; now: number }) {
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Building</th>
            <th scope="col">Status</th>
            <th scope="col" className="col-sensors">Sensors</th>
            <th scope="col" className="num col-battery">Floors</th>
            <th scope="col" className="num">Last update</th>
            <th scope="col" className="row-arrow">
              <span className="visually-hidden">Open</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const { building } = row;
            const href = buildingHref(building);
            return (
              <tr key={building.id} className="data-row" onClick={() => (window.location.hash = href)}>
                <th scope="row">
                  <a className="row-link" href={href} onClick={(e) => e.stopPropagation()}>
                    {addressOf(building)}
                    <span className="muted">
                      {building.zip} {building.city}
                    </span>
                  </a>
                </th>
                <td>
                  <StatusBadge status={row.status} />
                </td>
                <td className={`col-sensors${row.attention > 0 ? ' attention-text' : ''}`}>{sensorSummary(row)}</td>
                <td className="num col-battery">{building.floors}</td>
                <td className="num">{row.lastSeen ? formatAgo(now - row.lastSeen) : '–'}</td>
                <td className="row-arrow">
                  <Chevron />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function BuildingCard({ row, now }: { row: BuildingRow; now: number }) {
  const { building } = row;
  return (
    <a className={`card card--${row.status}`} href={buildingHref(building)}>
      <span className="card__header">
        <span>
          <span className="card__title">{addressOf(building)}</span>
          <span className="card__subtitle">
            {building.zip} {building.city} · {building.floors} floors
          </span>
        </span>
        <StatusBadge status={row.status} />
      </span>
      <span className="card__footer">
        <span className={row.attention > 0 ? 'attention-text' : undefined}>{sensorSummary(row)}</span>
        <span>{row.lastSeen ? formatAgo(now - row.lastSeen) : 'No data'}</span>
      </span>
    </a>
  );
}

export function BuildingPage({ buildings, sensors, now, buildingId }: Props & { buildingId: number }) {
  const row = useMemo(
    () => buildingRows(buildings.filter((b) => b.id === buildingId), sensors, now)[0],
    [buildings, sensors, now, buildingId],
  );

  if (!row) {
    return (
      <>
        <BackLink />
        <div className="empty">
          <strong>Building {buildingId} not found.</strong>
        </div>
      </>
    );
  }

  const { building } = row;
  const reporting = row.rows.filter((r) => r.status !== 'offline').length;
  const critical = row.rows.filter((r) => r.status === 'critical').length;

  return (
    <>
      <BackLink />
      <header className="building-header">
        <div>
          <h2 className="building-header__title">{addressOf(building)}</h2>
          <p className="page__subtitle">
            {building.zip} {building.city} · {building.floors} floors
          </p>
        </div>
        <StatusBadge status={row.status} />
      </header>

      <section className="tiles tiles--two" aria-label="Building summary">
        <StatTile label="Sensors reporting" value={reporting} detail={`of ${building.sensors.length} installed`} />
        <StatTile
          label="Critical"
          value={row.attention}
          tone={row.attention > 0 ? 'critical' : undefined}
          detail={
            row.attention === 0
              ? 'None'
              : [critical > 0 && `${critical} critical`, row.attention > critical && `${row.attention - critical} without data`]
                  .filter(Boolean)
                  .join(', ')
          }
        />
      </section>

      {row.missing.length > 0 && (
        <p className="notice" role="status">
          Never reported: {row.missing.map((s) => `Sensor ${s.id}`).join(', ')}. Installed in the database, but no
          MQTT message has arrived from {row.missing.length === 1 ? 'it' : 'them'} yet.
        </p>
      )}

      {building.sensors.length === 0 ? (
        <div className="empty">
          <strong>No sensors installed in this building.</strong>
        </div>
      ) : row.rows.length > 0 ? (
        <SensorList rows={row.rows} now={now} />
      ) : null}
    </>
  );
}

function BackLink() {
  return (
    <a className="back-link" href="#/">
      <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
        <path d="M10 3.5 5.5 8l4.5 4.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      All buildings
    </a>
  );
}

export function LoadingState({
  buildingsError,
  connection,
}: {
  buildingsError: string | null;
  connection: ConnectionState;
}) {
  if (connection === 'disconnected') {
    return (
      <div className="empty">
        <strong>Cannot reach the backend.</strong>
        <p>
          Start it with <code>mvn -N exec:exec</code> from the repository root.
        </p>
      </div>
    );
  }
  if (buildingsError) {
    return (
      <div className="empty">
        <strong>Cannot load buildings from the database.</strong>
        <p>
          Is PostgreSQL running? <code>docker compose up -d</code>
        </p>
        <p className="muted">{buildingsError}</p>
      </div>
    );
  }
  return (
    <div className="empty">
      <strong>Loading…</strong>
    </div>
  );
}
