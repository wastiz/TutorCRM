package com.tutormgmt.dashboard;

import com.tutormgmt.lesson.LessonDto;
import java.math.BigDecimal;
import java.util.List;

/** CLAUDE.md section 38. */
public record DashboardDto(
        long activeStudents,
        long todaysLessons,
        long thisWeekLessons,
        long thisMonthCompletedLessons,
        /** COMPLETED so far this month. */
        BigDecimal thisMonthEarnings,
        /** COMPLETED + still-PLANNED this month — what the tutor expects to be paid. */
        BigDecimal thisMonthExpectedEarnings,
        /** Today's lessons in chronological order — the tutor marks each one off from here. */
        List<LessonDto> todaysLessonList,
        List<LessonDto> upcomingLessons
) {}
