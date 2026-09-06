import { dayLabel, formatLabel, statusLabel, telegramUrl } from './student.labels';

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

  it('builds a telegram link from any of the shapes a tutor might paste', () => {
    expect(telegramUrl('anna_tutor')).toBe('https://t.me/anna_tutor');
    expect(telegramUrl('@anna_tutor')).toBe('https://t.me/anna_tutor');
    expect(telegramUrl('https://t.me/anna_tutor')).toBe('https://t.me/anna_tutor');
    expect(telegramUrl('t.me/anna_tutor?start=1')).toBe('https://t.me/anna_tutor');
  });

  it('has no link without a handle', () => {
    expect(telegramUrl(null)).toBeNull();
    expect(telegramUrl('  ')).toBeNull();
  });
});
