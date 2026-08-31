package com.tutormgmt.integration.google.calendar;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.tutormgmt.integration.google.GoogleApiFactory;
import com.tutormgmt.integration.google.GoogleErrors;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CLAUDE.md sections 30-33. Thin wrapper over the Google Calendar API — no domain logic,
 * no persistence. Callers pass a {@link CalendarEventData}; results are plain records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    static final String APP_PROPERTY = "application";
    static final String APP_PROPERTY_VALUE = "tutor-management";
    static final String LESSON_ID_PROPERTY = "lessonId";

    private final GoogleApiFactory apiFactory;

    public List<CalendarSummaryDto> listCalendars(UUID userId) {
        try {
            Calendar client = apiFactory.calendar(userId);
            List<CalendarSummaryDto> out = new ArrayList<>();
            String pageToken = null;
            do {
                var response = client.calendarList().list().setPageToken(pageToken).execute();
                for (CalendarListEntry e : response.getItems()) {
                    boolean writable = "owner".equals(e.getAccessRole()) || "writer".equals(e.getAccessRole());
                    out.add(new CalendarSummaryDto(e.getId(), e.getSummary(), e.getDescription(),
                            e.getAccessRole(), Boolean.TRUE.equals(e.getPrimary()), writable));
                }
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
            return out;
        } catch (IOException e) {
            throw GoogleErrors.translate("list your calendars", e);
        }
    }

    /** Timed events (not all-day) in the window, recurrences expanded. */
    public List<FetchedEvent> listEvents(UUID userId, String calendarId,
                                         OffsetDateTime from, OffsetDateTime to) {
        try {
            Calendar client = apiFactory.calendar(userId);
            List<FetchedEvent> out = new ArrayList<>();
            String pageToken = null;
            do {
                var response = client.events().list(calendarId)
                        .setTimeMin(new DateTime(from.toInstant().toEpochMilli()))
                        .setTimeMax(new DateTime(to.toInstant().toEpochMilli()))
                        .setSingleEvents(true)
                        .setOrderBy("startTime")
                        .setMaxResults(2500)
                        .setShowDeleted(false)
                        .setPageToken(pageToken)
                        .execute();
                for (Event e : response.getItems()) {
                    OffsetDateTime start = toOffset(e.getStart());
                    OffsetDateTime end = toOffset(e.getEnd());
                    if (start == null || end == null || "cancelled".equals(e.getStatus())) {
                        continue; // all-day or cancelled
                    }
                    Map<String, String> priv = e.getExtendedProperties() == null
                            ? Map.of()
                            : Optional.ofNullable(e.getExtendedProperties().getPrivate()).orElse(Map.of());
                    boolean createdByApp = APP_PROPERTY_VALUE.equals(priv.get(APP_PROPERTY));
                    UUID lessonId = parseUuid(priv.get(LESSON_ID_PROPERTY));
                    List<String> attendees = e.getAttendees() == null ? List.of()
                            : e.getAttendees().stream().map(EventAttendee::getEmail)
                              .filter(a -> a != null).map(String::toLowerCase).toList();
                    out.add(new FetchedEvent(e.getId(), calendarId, e.getSummary(), e.getDescription(),
                            start, end, lessonId, createdByApp, attendees, e.getHtmlLink()));
                }
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
            return out;
        } catch (IOException e) {
            throw GoogleErrors.translate("read events from your calendar", e);
        }
    }

    /** Tags an existing (user-created) event as managed by a lesson, preserving other private props. */
    public void linkEventToLesson(UUID userId, String calendarId, String eventId, UUID lessonId) {
        try {
            Calendar client = apiFactory.calendar(userId);
            Event existing = client.events().get(calendarId, eventId).execute();
            Map<String, String> priv = new HashMap<>();
            if (existing.getExtendedProperties() != null && existing.getExtendedProperties().getPrivate() != null) {
                priv.putAll(existing.getExtendedProperties().getPrivate());
            }
            priv.put(APP_PROPERTY, APP_PROPERTY_VALUE);
            priv.put(LESSON_ID_PROPERTY, lessonId.toString());
            Event patch = new Event().setExtendedProperties(
                    new Event.ExtendedProperties().setPrivate(priv));
            client.events().patch(calendarId, eventId, patch).execute();
        } catch (IOException e) {
            throw GoogleErrors.translate("link the calendar event to a lesson", e);
        }
    }

    private static OffsetDateTime toOffset(EventDateTime edt) {
        if (edt == null || edt.getDateTime() == null) {
            return null;
        }
        return Instant.ofEpochMilli(edt.getDateTime().getValue()).atOffset(ZoneOffset.UTC);
    }

    private static UUID parseUuid(String s) {
        try {
            return s == null ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record FetchedEvent(String id, String calendarId, String summary, String description,
                               OffsetDateTime start, OffsetDateTime end, UUID linkedLessonId,
                               boolean createdByApp, List<String> attendeeEmails, String htmlLink) {}

    public SyncedEvent createEvent(UUID userId, String calendarId, CalendarEventData data) {
        try {
            Event created = apiFactory.calendar(userId).events()
                    .insert(calendarId, toEvent(new Event(), data)).execute();
            return new SyncedEvent(calendarId, created.getId(), created.getHtmlLink());
        } catch (IOException e) {
            throw GoogleErrors.translate("create a calendar event", e);
        }
    }

    public SyncedEvent updateEvent(UUID userId, String calendarId, String eventId, CalendarEventData data) {
        try {
            Calendar client = apiFactory.calendar(userId);
            Event existing;
            try {
                existing = client.events().get(calendarId, eventId).execute();
            } catch (IOException notFound) {
                return createEvent(userId, calendarId, data);
            }
            Event updated = client.events().update(calendarId, eventId, toEvent(existing, data)).execute();
            return new SyncedEvent(calendarId, updated.getId(), updated.getHtmlLink());
        } catch (IOException e) {
            throw GoogleErrors.translate("update the calendar event", e);
        }
    }

    public void deleteEvent(UUID userId, String calendarId, String eventId) {
        try {
            apiFactory.calendar(userId).events().delete(calendarId, eventId).execute();
        } catch (IOException e) {
            if (e instanceof com.google.api.client.googleapis.json.GoogleJsonResponseException g
                    && (g.getStatusCode() == 404 || g.getStatusCode() == 410)) {
                log.info("Calendar event {} already gone", eventId);
                return;
            }
            throw GoogleErrors.translate("delete the calendar event", e);
        }
    }

    private static Event toEvent(Event event, CalendarEventData data) {
        event.setSummary(data.summary());
        event.setDescription(data.description());
        event.setStart(new EventDateTime().setDateTime(new DateTime(data.start().toInstant().toEpochMilli()))
                .setTimeZone(data.start().getOffset().getId()));
        event.setEnd(new EventDateTime().setDateTime(new DateTime(data.end().toInstant().toEpochMilli()))
                .setTimeZone(data.end().getOffset().getId()));
        Event.ExtendedProperties props = event.getExtendedProperties() != null
                ? event.getExtendedProperties() : new Event.ExtendedProperties();
        props.setPrivate(Map.of(
                APP_PROPERTY, APP_PROPERTY_VALUE,
                LESSON_ID_PROPERTY, data.lessonId().toString()));
        event.setExtendedProperties(props);
        return event;
    }

    public record SyncedEvent(String calendarId, String eventId, String htmlLink) {}
}
