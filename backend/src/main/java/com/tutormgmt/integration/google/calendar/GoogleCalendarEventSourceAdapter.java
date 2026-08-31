package com.tutormgmt.integration.google.calendar;

import com.tutormgmt.authentication.AuthenticationService;
import com.tutormgmt.lesson.CalendarEventSource;
import com.tutormgmt.settings.UserSettings;
import com.tutormgmt.settings.UserSettingsRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Google-side implementation of the lesson slice's {@link CalendarEventSource} port. */
@Component
@RequiredArgsConstructor
public class GoogleCalendarEventSourceAdapter implements CalendarEventSource {

    private final AuthenticationService authenticationService;
    private final UserSettingsRepository settingsRepository;
    private final GoogleCalendarService calendarService;

    @Override
    public boolean enabledFor(UUID userId) {
        return authenticationService.isConnected(userId) && targetCalendarId(userId) != null;
    }

    @Override
    public String targetCalendarId(UUID userId) {
        return settingsRepository.findByUserId(userId)
                .map(UserSettings::getCalendarId)
                .filter(id -> id != null && !id.isBlank())
                .orElse(null);
    }

    @Override
    public List<ExternalEvent> fetch(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        String calendarId = targetCalendarId(userId);
        if (calendarId == null || !authenticationService.isConnected(userId)) {
            return List.of();
        }
        return calendarService.listEvents(userId, calendarId, from, to).stream()
                .map(e -> new ExternalEvent(e.id(), e.calendarId(), e.summary(), e.description(),
                        e.start(), e.end(), e.linkedLessonId(), e.createdByApp(), e.attendeeEmails()))
                .toList();
    }

    @Override
    public void link(UUID userId, String calendarId, String eventId, UUID lessonId) {
        calendarService.linkEventToLesson(userId, calendarId, eventId, lessonId);
    }
}
