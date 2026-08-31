package com.tutormgmt.lesson;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Generate follow-up lessons from an existing one (CLAUDE.md section 36).
 * Each generated lesson is a separate record with its own calendar event.
 */
public record RepeatLessonRequest(
        /** How many additional weekly lessons to create after the source lesson. */
        @Min(1) @Max(52) int occurrences,
        /** Weeks between lessons (1 = weekly, 2 = fortnightly). */
        @Min(1) @Max(8) Integer intervalWeeks
) {
    public int intervalWeeksOrDefault() {
        return intervalWeeks == null ? 1 : intervalWeeks;
    }
}
