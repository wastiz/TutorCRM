import { dayLabel, formatLabel, statusLabel } from './student.labels';

describe('student labels', () => {
  it('maps known values', () => {
    expect(formatLabel('BOTH')).toBe('Online + Offline');
    expect(statusLabel('ACTIVE')).toBe('Active');
    expect(dayLabel('THURSDAY')).toBe('Thursday');
  });

  it('falls back to an em dash for null/undefined', () => {
    expect(formatLabel(null)).toBe('—');
    expect(statusLabel(undefined)).toBe('—');
    expect(dayLabel(null)).toBe('—');
  });
});
