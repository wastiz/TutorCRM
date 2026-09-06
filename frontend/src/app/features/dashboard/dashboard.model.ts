import { Lesson } from '../lessons/lesson.model';

export interface Dashboard {
  activeStudents: number;
  todaysLessons: number;
  thisWeekLessons: number;
  thisMonthCompletedLessons: number;
  thisMonthEarnings: number;
  thisMonthExpectedEarnings: number;
  /** Today's lessons, chronological — marked off straight from the dashboard. */
  todaysLessonList: Lesson[];
  upcomingLessons: Lesson[];
}
