package com.tutormgmt.lesson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read side of the Google Calendar integration: lets the lesson slice pull events
 * the tutor created directly in their calendar (Google → app). Implemented in
 * {@code integration.google.calendar}; the lesson slice imports no Google classes.
 */
public interface CalendarEventSource {

    boolean enabledFor(UUID userId);

    /** The calendar chosen in Settings, or {@code null}. */
    String targetCalendarId(UUID userId);

    List<ExternalEvent> fetch(UUID userId, OffsetDateTime from, OffsetDateTime to);

    /** Writes the {@code application}/{@code lessonId} tags back onto a user-created event. */
    void link(UUID userId, String calendarId, String eventId, UUID lessonId);

    record ExternalEvent(
            String eventId,
            String calendarId,
            String summary,
            String description,
            OffsetDateTime start,
            OffsetDateTime end,
            /** Non-null when the event already carries our {@code lessonId} tag. */
            UUID linkedLessonId,
            boolean createdByApp,
            List<String> attendeeEmails
    ) {}
}
