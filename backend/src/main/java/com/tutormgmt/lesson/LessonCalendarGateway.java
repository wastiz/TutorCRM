package com.tutormgmt.lesson;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for mirroring a lesson to Google Calendar. The implementation lives in
 * {@code integration.google.calendar} so the lesson slice never imports Google classes
 * (CLAUDE.md section 63) and stays unit-testable.
 */
public interface LessonCalendarGateway {

    /** True when the user has connected Google and chosen a target calendar. */
    boolean enabledFor(UUID userId);

    /** Create or update the calendar event. Empty when calendar sync is not enabled. */
    Optional<CalendarLink> sync(UUID userId, LessonSyncCommand command);

    /** Best-effort delete; never throws for an already-missing event. */
    void remove(UUID userId, String calendarId, String eventId);

    record CalendarLink(String calendarId, String eventId) {}

    record LessonSyncCommand(
            UUID lessonId,
            String studentName,
            OffsetDateTime start,
            OffsetDateTime end,
            LessonStatus status,
            String notes,
            String existingCalendarId,
            String existingEventId
    ) {}
}
