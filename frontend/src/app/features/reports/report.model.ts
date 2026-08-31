export interface StudentReportRow {
  fullName: string;
  studentNumber: string;
  email: string | null;
  parentName: string | null;
  isikukood: string | null;
  subject: string | null;
  lessonCount: number;
  lessonPrice: number | null;
  total: number;
}

export interface MonthlyReport {
  month: string; // "yyyy-MM"
  title: string; // "август 26"
  students: StudentReportRow[];
  totalLessons: number;
  totalAmount: number;
  warnings: string[];
  errors: string[];
}
