package com.tutormgmt.integration.google.calendar;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.tutormgmt.integration.google.GoogleApiFactory;
import com.tutormgmt.integration.google.GoogleErrors;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
