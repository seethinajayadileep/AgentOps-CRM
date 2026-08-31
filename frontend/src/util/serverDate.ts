/**
 * API timestamps are stored as JVM wall-clock LocalDateTime and serialized as
 * ISO-8601 instants. Older payloads omit the offset; browsers treat those as
 * local time, which shifts relative labels by the user's UTC offset (e.g. IST).
 * Treat timezone-less datetimes as UTC.
 */

const HAS_ZONE = /[zZ]$|[+-]\d{2}:?\d{2}$/;
const DATE_ONLY = /^\d{4}-\d{2}-\d{2}$/;

export function parseServerDate(value: string | null | undefined): Date | null {
  if (value == null || value === '') return null;
  const trimmed = value.trim();
  const normalized = DATE_ONLY.test(trimmed)
    ? `${trimmed}T00:00:00Z`
    : HAS_ZONE.test(trimmed)
      ? trimmed
      : `${trimmed}Z`;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return null;
  return date;
}

export function formatServerDateTime(
  value: string | null | undefined,
  locales?: Intl.LocalesArgument,
  options?: Intl.DateTimeFormatOptions
): string {
  const date = parseServerDate(value);
  if (!date) return value || '';
  return date.toLocaleString(locales, options);
}

export function formatServerDate(
  value: string | null | undefined,
  locales?: Intl.LocalesArgument,
  options?: Intl.DateTimeFormatOptions
): string {
  const date = parseServerDate(value);
  if (!date) return value || '';
  return date.toLocaleDateString(locales, options);
}

export function formatServerTime(
  value: string | null | undefined,
  locales?: Intl.LocalesArgument,
  options?: Intl.DateTimeFormatOptions
): string {
  const date = parseServerDate(value);
  if (!date) return value || '';
  return date.toLocaleTimeString(locales, options);
}

export function isOlderThan(
  value: string | null | undefined,
  maxAgeMs: number,
  now = Date.now()
): boolean {
  const date = parseServerDate(value);
  if (!date) return false;
  return now - date.getTime() > maxAgeMs;
}

export function formatRelativeTime(
  value: string | null | undefined,
  options?: { style?: 'compact' | 'long'; now?: number; empty?: string }
): string {
  const empty = options?.empty ?? '';
  const date = parseServerDate(value);
  if (!date) return value || empty;
  const now = options?.now ?? Date.now();
  const diffMs = now - date.getTime();
  const minutes = Math.max(0, Math.round(diffMs / 60000));
  const compact = options?.style !== 'long';

  if (compact) {
    const mins = Math.max(0, Math.floor(diffMs / 60000));
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  }

  if (minutes < 1) return 'Just now';
  if (minutes === 1) return '1 min ago';
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours === 1) return '1 hr ago';
  if (hours < 24) return `${hours} hr ago`;
  const days = Math.round(hours / 24);
  if (days === 1) return 'Yesterday';
  if (days < 7) return `${days} days ago`;
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
