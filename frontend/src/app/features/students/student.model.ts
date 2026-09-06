export type StudentStatus = 'ACTIVE' | 'PAUSED' | 'FINISHED';
export type LessonFormat = 'ONLINE' | 'OFFLINE' | 'BOTH';
export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export interface ScheduleEntry {
  dayOfWeek: DayOfWeek | null;
  startTime: string | null; // "HH:mm"
  endTime: string | null;
  lessonsPerWeek: number | null;
}

export interface StudentRequest {
  firstName: string;
  lastName: string;
  email?: string | null;
  phone?: string | null;
  isikukood?: string | null;
  age?: number | null;
  grade?: number | null;
  school?: string | null;
  subject?: string | null;
  goal?: string | null;
  level?: string | null;
  notes?: string | null;
  lessonFormat?: LessonFormat | null;
  lessonPrice?: number | null;
  status?: StudentStatus | null;
  startDate?: string | null;
  endDate?: string | null;
  leaveReason?: string | null;
  parentName?: string | null;
  parentPhone?: string | null;
  parentEmail?: string | null;
  parentSecondaryPhone?: string | null;
  parentIsikukood?: string | null;
  telegram?: string | null;
  schedules: ScheduleEntry[];
  ignoreDuplicates?: boolean;
}

export interface StudentScheduleView {
  dayOfWeek: DayOfWeek;
  startTime: string | null;
  endTime: string | null;
  lessonsPerWeek: number | null;
}

export interface Student {
  id: string;
  studentNumber: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  isikukood: string | null;
  age: number | null;
  grade: number | null;
  school: string | null;
  subject: string | null;
  goal: string | null;
  level: string | null;
  notes: string | null;
  lessonFormat: LessonFormat | null;
  lessonPrice: number | null;
  status: StudentStatus;
  startDate: string | null;
  endDate: string | null;
  leaveReason: string | null;
  parentName: string | null;
  parentPhone: string | null;
  parentEmail: string | null;
  parentSecondaryPhone: string | null;
  parentIsikukood: string | null;
  telegram: string | null;
  schedules: StudentScheduleView[];
  createdAt: string;
  updatedAt: string;
}

export interface StudentSummary {
  id: string;
  studentNumber: string;
  fullName: string;
  subject: string | null;
  grade: number | null;
  lessonFormat: LessonFormat | null;
  lessonPrice: number | null;
  status: StudentStatus;
  email: string | null;
  phone: string | null;
  telegram: string | null;
  nextLessonAt: string | null;
}

export interface ImportedStudent extends StudentRequest {
  preferredDays: DayOfWeek[];
  preferredTimeFrom: string | null;
  preferredTimeTo: string | null;
  preferredTimeRaw: string | null;
  lessonsPerWeek: number | null;
}

export interface StudentImportPreview {
  student: ImportedStudent;
  warnings: string[];
  fieldCount: number;
  rawText: string;
}

export interface DuplicateCheckRequest {
  email?: string | null;
  phone?: string | null;
  firstName?: string | null;
  lastName?: string | null;
}
