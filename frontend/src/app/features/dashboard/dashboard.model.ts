import { Lesson } from '../lessons/lesson.model';

export interface Dashboard {
  activeStudents: number;
  todaysLessons: number;
  thisWeekLessons: number;
  thisMonthCompletedLessons: number;
  thisMonthEarnings: number;
  thisMonthExpectedEarnings: number;
  upcomingLessons: Lesson[];
}
