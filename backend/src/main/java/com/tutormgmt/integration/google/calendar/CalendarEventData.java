package com.tutormgmt.integration.google.calendar;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Everything the calendar layer needs from a lesson, without importing the lesson entity. */
public record CalendarEventData(
        UUID lessonId,
        String summary,
        String description,
        OffsetDateTime start,
        OffsetDateTime end
) {}
