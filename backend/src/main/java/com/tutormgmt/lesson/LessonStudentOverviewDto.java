package com.tutormgmt.lesson;

import java.math.BigDecimal;
import java.util.List;

/** Lesson section of the Student details page (CLAUDE.md section 40). */
public record LessonStudentOverviewDto(
        List<LessonDto> upcoming,
        List<LessonDto> past,
        int completedThisMonth,
        BigDecimal earningsThisMonth,
        BigDecimal earningsTotal
) {}
