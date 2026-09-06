package com.tutormgmt.integration.google.calendar;

import com.tutormgmt.authentication.AuthenticationService;
import com.tutormgmt.lesson.LessonCalendarGateway;
import com.tutormgmt.lesson.LessonPricing;
import com.tutormgmt.settings.UserSettings;
import com.tutormgmt.settings.UserSettingsRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Wires the lesson slice's {@link LessonCalendarGateway} port to Google Calendar. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarLessonAdapter implements LessonCalendarGateway {

    private final AuthenticationService authenticationService;
    private final UserSettingsRepository settingsRepository;
    private final GoogleCalendarService calendarService;

    @Override
    public boolean enabledFor(UUID userId) {
        return authenticationService.isConnected(userId) && targetCalendarId(userId).isPresent();
    }

    @Override
    public Optional<CalendarLink> sync(UUID userId, LessonSyncCommand cmd) {
        Optional<String> target = targetCalendarId(userId);
        if (!authenticationService.isConnected(userId) || target.isEmpty()) {
            return Optional.empty();
        }
        String calendarId = target.get();
        CalendarEventData data = new CalendarEventData(
                cmd.lessonId(), summary(cmd), description(cmd), cmd.start(), cmd.end());

        GoogleCalendarService.SyncedEvent event;
        if (cmd.existingEventId() != null && calendarId.equals(cmd.existingCalendarId())) {
            event = calendarService.updateEvent(userId, calendarId, cmd.existingEventId(), data);
        } else {
            if (cmd.existingEventId() != null && cmd.existingCalendarId() != null) {
                // target calendar changed — drop the old event first
                calendarService.deleteEvent(userId, cmd.existingCalendarId(), cmd.existingEventId());
            }
            event = calendarService.createEvent(userId, calendarId, data);
        }
        return Optional.of(new CalendarLink(event.calendarId(), event.eventId()));
    }

    @Override
    public void remove(UUID userId, String calendarId, String eventId) {
        if (calendarId == null || eventId == null || !authenticationService.isConnected(userId)) {
            return;
        }
        calendarService.deleteEvent(userId, calendarId, eventId);
    }

    private Optional<String> targetCalendarId(UUID userId) {
        return settingsRepository.findByUserId(userId)
                .map(UserSettings::getCalendarId)
                .filter(id -> id != null && !id.isBlank());
    }

    /**
     * Title carries the student and the lesson length. The price is deliberately absent —
     * a shared calendar should not show what the tutor is paid (owner request, 2026-09-05).
     */
    private static String summary(LessonSyncCommand cmd) {
        String name = cmd.studentName() == null || cmd.studentName().isBlank() ? "Lesson" : cmd.studentName();
        return "Lesson · " + name + " · " + durationLabel(cmd);
    }

    private static String description(LessonSyncCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Duration: ").append(durationLabel(cmd)).append('\n');
        sb.append("Status: ").append(cmd.status()).append('\n');
        if (cmd.notes() != null && !cmd.notes().isBlank()) {
            sb.append('\n').append(cmd.notes());
        }
        sb.append("\n\n— created by tutor-management");
        return sb.toString();
    }

    /** "1 h", "1.5 h" or "45 min". */
    static String durationLabel(LessonSyncCommand cmd) {
        long minutes = LessonPricing.minutesBetween(cmd.start(), cmd.end());
        if (minutes % 60 == 0) {
            return (minutes / 60) + " h";
        }
        return minutes == 90 ? "1.5 h" : minutes + " min";
    }
}
