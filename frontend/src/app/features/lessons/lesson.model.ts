export type LessonStatus = 'PLANNED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type CalendarSyncStatus = 'PENDING' | 'SYNCED' | 'FAILED' | 'DISABLED';

export interface Lesson {
  id: string;
  studentId: string;
  studentName: string | null;
  startTime: string; // ISO offset date-time
  endTime: string;
  price: number;
  status: LessonStatus;
  notes: string | null;
  googleCalendarId: string | null;
  googleCalendarEventId: string | null;
  calendarSyncStatus: CalendarSyncStatus;
  createdAt: string;
  updatedAt: string;
}

export interface LessonRequest {
  studentId: string;
  startTime: string;
  endTime: string;
  price?: number | null;
  status?: LessonStatus | null;
  notes?: string | null;
}

export interface RepeatLessonRequest {
  occurrences: number;
  intervalWeeks?: number;
}

export interface LessonStudentOverview {
  upcoming: Lesson[];
  past: Lesson[];
  completedThisMonth: number;
  earningsThisMonth: number;
  earningsTotal: number;
}

export const LESSON_STATUS_LABEL: Record<LessonStatus, string> = {
  PLANNED: 'Planned',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  NO_SHOW: 'No show',
};

export const LESSON_STATUS_COLOR: Record<LessonStatus, string> = {
  PLANNED: '#3f51b5',
  COMPLETED: '#2e7d32',
  CANCELLED: '#9e9e9e',
  NO_SHOW: '#c62828',
};
