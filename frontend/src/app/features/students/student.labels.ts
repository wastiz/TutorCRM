import { DayOfWeek, LessonFormat, StudentStatus } from './student.model';

const FORMAT: Record<LessonFormat, string> = {
  ONLINE: 'Online',
  OFFLINE: 'Offline',
  BOTH: 'Online + Offline',
};

const STATUS: Record<StudentStatus, string> = {
  ACTIVE: 'Active',
  PAUSED: 'Paused',
  FINISHED: 'Finished',
};

const DAY: Record<DayOfWeek, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
};

export function formatLabel(value: LessonFormat | null | undefined): string {
  return value ? FORMAT[value] : '—';
}

export function statusLabel(value: StudentStatus | null | undefined): string {
  return value ? STATUS[value] : '—';
}

export function dayLabel(value: DayOfWeek | null | undefined): string {
  return value ? DAY[value] : '—';
}

/**
 * Link to a Telegram chat. The username is stored bare, but a value pasted as
 * "@name" or a full t.me link is tolerated here too so old rows keep working.
 */
export function telegramUrl(handle: string | null | undefined): string | null {
  if (!handle) {
    return null;
  }
  const bare = handle
    .trim()
    .replace(/^(https?:\/\/)?(t(elegram)?\.me|telegram\.dog)\//i, '')
    .replace(/^@/, '')
    .split('?')[0];
  return bare ? `https://t.me/${bare}` : null;
}
