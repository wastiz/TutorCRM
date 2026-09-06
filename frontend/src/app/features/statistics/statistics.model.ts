export interface StudentCounts {
  total: number;
  active: number;
  paused: number;
  finished: number;
}

export interface LessonCounts {
  total: number;
  completed: number;
  cancelled: number;
  noShow: number;
  planned: number;
  cancellationRate: number;
}

export interface LeaveReason {
  studentId: string;
  studentName: string;
  endDate: string | null;
  reason: string;
}

export interface MonthPoint {
  month: string; // "2026-09"
  completed: number;
  cancelled: number;
  noShow: number;
  earnings: number;
}

export interface Statistics {
  students: StudentCounts;
  lessons: LessonCounts;
  totalEarnings: number;
  leaveReasons: LeaveReason[];
  byMonth: MonthPoint[];
}
