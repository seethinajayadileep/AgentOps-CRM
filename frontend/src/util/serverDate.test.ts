import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  formatRelativeTime,
  formatServerDateTime,
  isOlderThan,
  parseServerDate,
} from './serverDate';

describe('parseServerDate', () => {
  it('treats timezone-less datetimes as UTC, not the browser local zone', () => {
    const parsed = parseServerDate('2026-08-31T13:00:00');
    expect(parsed?.toISOString()).toBe('2026-08-31T13:00:00.000Z');
  });

  it('keeps an explicit offset', () => {
    const parsed = parseServerDate('2026-08-31T18:30:00+05:30');
    expect(parsed?.toISOString()).toBe('2026-08-31T13:00:00.000Z');
  });

  it('keeps a trailing Z', () => {
    const parsed = parseServerDate('2026-08-31T13:00:00Z');
    expect(parsed?.toISOString()).toBe('2026-08-31T13:00:00.000Z');
  });
});

describe('formatRelativeTime', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('does not shift a just-created UTC wall-clock timestamp by the IST offset', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-31T13:30:00.000Z'));
    expect(formatRelativeTime('2026-08-31T13:29:00')).toBe('1m ago');
    expect(formatRelativeTime('2026-08-31T13:29:00', { style: 'long' })).toBe('1 min ago');
    expect(formatRelativeTime('2026-08-31T13:29:00')).not.toMatch(/5h|5 hr|6 hr/);
  });

  it('shows just now for timestamps under a minute old', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-31T13:30:00.000Z'));
    expect(formatRelativeTime('2026-08-31T13:29:30')).toBe('Just now');
  });
});

describe('formatServerDateTime', () => {
  it('returns empty for missing values', () => {
    expect(formatServerDateTime(undefined)).toBe('');
  });
});

describe('isOlderThan', () => {
  it('uses UTC for timezone-less timestamps', () => {
    const now = Date.parse('2026-08-31T13:30:00.000Z');
    expect(isOlderThan('2026-08-31T13:00:00', 60 * 60 * 1000, now)).toBe(false);
    expect(isOlderThan('2026-08-31T12:00:00', 60 * 60 * 1000, now)).toBe(true);
  });
});
