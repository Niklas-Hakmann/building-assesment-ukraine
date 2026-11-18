// Shared, reusable UI components.
import type { ReactNode } from 'react';
import type { SensorStatus } from '../types';

const STATUS_LABEL: Record<SensorStatus, string> = {
  critical: 'Critical',
  offline: 'No data',
  normal: 'Normal',
};

/** Status is always icon + label, never colour alone. */
export function StatusBadge({ status }: { status: SensorStatus }) {
  return (
    <span className={`status status--${status}`}>
      <StatusIcon status={status} />
      {STATUS_LABEL[status]}
    </span>
  );
}

function StatusIcon({ status }: { status: SensorStatus }) {
  const common = {
    width: 14,
    height: 14,
    viewBox: '0 0 16 16',
    'aria-hidden': true,
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  };
  switch (status) {
    case 'critical':
      return (
        <svg {...common}>
          <path d="M8 1.8 15 14H1z" />
          <path d="M8 6.5v3.2M8 11.8v.1" />
        </svg>
      );
    case 'offline':
      return (
        <svg {...common}>
          <circle cx="8" cy="8" r="6.2" />
          <path d="M8 4.5V8l2.3 1.6" />
        </svg>
      );
    default:
      return (
        <svg {...common}>
          <circle cx="8" cy="8" r="6.2" />
          <path d="m5.3 8.2 1.8 1.8 3.6-3.8" />
        </svg>
      );
  }
}

export function StatTile({
  label,
  value,
  detail,
  tone,
}: {
  label: string;
  value: ReactNode;
  detail?: ReactNode;
  tone?: SensorStatus;
}) {
  return (
    <div className={`tile${tone ? ` tile--${tone}` : ''}`}>
      <div className="tile__label">{label}</div>
      <div className="tile__value">{value}</div>
      {detail && <div className="tile__detail">{detail}</div>}
    </div>
  );
}

export function SegmentedControl<T extends string>({
  label,
  options,
  value,
  onChange,
}: {
  label: string;
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <div className="segmented" role="radiogroup" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={value === option.value}
          className={value === option.value ? 'is-active' : ''}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

export function formatAgo(ms: number): string {
  const s = Math.max(0, Math.round(ms / 1000));
  if (s < 60) return `${s} s ago`;
  const m = Math.floor(s / 60);
  if (m < 60) return `${m} min ago`;
  return `${Math.floor(m / 60)} h ago`;
}

export function formatNumber(value: number, digits = 0): string {
  return value.toLocaleString('en-GB', { minimumFractionDigits: digits, maximumFractionDigits: digits });
}
