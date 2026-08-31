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

// --- Google Calendar -> app import ---

export interface GoogleImportNewEvent {
  eventId: string;
  calendarId: string;
  summary: string | null;
  start: string;
  end: string;
  suggestedStudentId: string | null;
  suggestedStudentName: string | null;
  suggestedPrice: number | null;
}

export interface GoogleImportMovedLesson {
  lessonId: string;
  studentName: string;
  eventId: string;
  currentStart: string;
  currentEnd: string;
  newStart: string;
  newEnd: string;
}

export interface GoogleImportPreview {
  enabled: boolean;
  from: string;
  to: string;
  newEvents: GoogleImportNewEvent[];
  movedLessons: GoogleImportMovedLesson[];
  alreadyLinked: number;
}

export interface GoogleImportRequest {
  from: string;
  to: string;
  items: { eventId: string; studentId: string; price?: number | null }[];
  updateMoved: boolean;
}

export interface GoogleImportResult {
  imported: number;
  updated: number;
}
