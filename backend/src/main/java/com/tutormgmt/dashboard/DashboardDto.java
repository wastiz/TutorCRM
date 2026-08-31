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
        List<LessonDto> upcomingLessons
) {}
