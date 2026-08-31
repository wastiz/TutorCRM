package com.tutormgmt.lesson;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.student.Student;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Pulls events the tutor created directly in Google Calendar into the app as lessons
 * (Google → app). Deliberately a review-then-confirm flow, like the message import —
 * student matching from a free-text event title is only a guess.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonImportService {

    private final CalendarEventSource eventSource;
    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public GoogleImportDto.Preview preview(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        if (!eventSource.enabledFor(userId)) {
            return new GoogleImportDto.Preview(false, from, to, List.of(), List.of(), 0);
        }

        List<CalendarEventSource.ExternalEvent> events = eventSource.fetch(userId, from, to);
        List<Student> students = studentRepository.findByUserIdOrderByStudentNumberAsc(userId).stream()
                .filter(s -> s.getStatus() != StudentStatus.FINISHED)
                .toList();
        Map<String, Lesson> lessonsByEventId = lessonRepository.findByUserIdOrderByStartTimeAsc(userId).stream()
                .filter(l -> l.getGoogleCalendarEventId() != null)
                .collect(Collectors.toMap(Lesson::getGoogleCalendarEventId, l -> l, (a, b) -> a));

        List<GoogleImportDto.NewEvent> newEvents = new ArrayList<>();
        List<GoogleImportDto.MovedLesson> moved = new ArrayList<>();
        int alreadyLinked = 0;

        for (CalendarEventSource.ExternalEvent e : events) {
            Lesson linked = lessonsByEventId.get(e.eventId());
            if (linked != null) {
                alreadyLinked++;
                if (!linked.getStartTime().isEqual(e.start()) || !linked.getEndTime().isEqual(e.end())) {
                    moved.add(new GoogleImportDto.MovedLesson(linked.getId(),
                            nameOf(students, linked.getStudentId()), e.eventId(),
                            linked.getStartTime(), linked.getEndTime(), e.start(), e.end()));
                }
                continue;
            }
            if (e.createdByApp()) {
                continue; // our own event whose lesson was deleted — ignore
            }
            Student guess = suggestStudent(students, e);
            newEvents.add(new GoogleImportDto.NewEvent(
                    e.eventId(), e.calendarId(), e.summary(), e.start(), e.end(),
                    guess == null ? null : guess.getId(),
                    guess == null ? null : fullName(guess),
                    guess == null ? null : guess.getLessonPrice()));
        }

        return new GoogleImportDto.Preview(true, from, to, newEvents, moved, alreadyLinked);
    }

    @Transactional
    public GoogleImportDto.ImportResult importSelected(UUID userId, GoogleImportDto.ImportRequest request) {
        if (!eventSource.enabledFor(userId)) {
            throw ApiException.badRequest("CALENDAR_NOT_CONFIGURED",
                    "Connect Google and pick a calendar in Settings first");
        }
        String calendarId = eventSource.targetCalendarId(userId);

        Map<String, CalendarEventSource.ExternalEvent> byId = eventSource
                .fetch(userId, request.from(), request.to()).stream()
                .collect(Collectors.toMap(CalendarEventSource.ExternalEvent::eventId, e -> e, (a, b) -> a));
        Set<String> alreadyLinked = lessonRepository.findByUserIdOrderByStartTimeAsc(userId).stream()
                .map(Lesson::getGoogleCalendarEventId).filter(id -> id != null).collect(Collectors.toSet());

        int imported = 0;
        for (GoogleImportDto.ImportRequest.Item item : request.items()) {
            CalendarEventSource.ExternalEvent e = byId.get(item.eventId());
            if (e == null || alreadyLinked.contains(item.eventId())) {
                continue;
            }
            Student student = studentRepository.findByIdAndUserId(item.studentId(), userId)
                    .orElseThrow(() -> ApiException.badRequest("STUDENT_NOT_FOUND", "Student not found"));

            Lesson lesson = Lesson.create(userId, student.getId());
            lesson.setStartTime(e.start());
            lesson.setEndTime(e.end());
            lesson.setPrice(resolvePrice(item.price(), student));
            lesson.setStatus(LessonStatus.PLANNED);
            lesson.setNotes(StringUtils.hasText(e.description()) ? e.description().strip() : null);
            lesson.setGoogleCalendarId(calendarId);
            lesson.setGoogleCalendarEventId(e.eventId());
            lesson.setCalendarSyncStatus(CalendarSyncStatus.SYNCED);
            lesson = lessonRepository.save(lesson);

            try {
                eventSource.link(userId, calendarId, e.eventId(), lesson.getId());
            } catch (RuntimeException ex) {
                log.warn("Imported lesson {} but could not tag the Google event: {}",
                        lesson.getId(), ex.getMessage());
            }
            imported++;
        }

        int updated = 0;
        if (request.updateMoved()) {
            Map<String, Lesson> linkedLessons = lessonRepository.findByUserIdOrderByStartTimeAsc(userId).stream()
                    .filter(l -> l.getGoogleCalendarEventId() != null)
                    .collect(Collectors.toMap(Lesson::getGoogleCalendarEventId, l -> l, (a, b) -> a));
            for (CalendarEventSource.ExternalEvent e : byId.values()) {
                Lesson lesson = linkedLessons.get(e.eventId());
                if (lesson != null
                        && (!lesson.getStartTime().isEqual(e.start()) || !lesson.getEndTime().isEqual(e.end()))) {
                    lesson.setStartTime(e.start());
                    lesson.setEndTime(e.end());
                    lessonRepository.save(lesson);
                    updated++;
                }
            }
        }

        log.info("Google import for user {}: {} new lesson(s), {} moved lesson(s) updated", userId, imported, updated);
        return new GoogleImportDto.ImportResult(imported, updated);
    }

    // --- matching ---

    private Student suggestStudent(List<Student> students, CalendarEventSource.ExternalEvent e) {
        String haystack = (e.summary() == null ? "" : e.summary()).toLowerCase(Locale.ROOT);
        Set<String> attendees = e.attendeeEmails() == null ? Set.of()
                : e.attendeeEmails().stream().map(a -> a.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        List<Student> matches = new ArrayList<>();
        for (Student s : students) {
            String first = safe(s.getFirstName());
            String last = safe(s.getLastName());
            boolean byName = !haystack.isBlank() && (
                    haystack.contains((first + " " + last).trim())
                            || haystack.contains((last + " " + first).trim())
                            || (first.length() >= 3 && wordMatch(haystack, first))
                            || (last.length() >= 3 && wordMatch(haystack, last)));
            boolean byEmail = s.getEmail() != null && attendees.contains(s.getEmail().toLowerCase(Locale.ROOT));
            if (byName || byEmail) {
                matches.add(s);
            }
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static boolean wordMatch(String haystack, String word) {
        int i = haystack.indexOf(word);
        while (i >= 0) {
            boolean leftOk = i == 0 || !Character.isLetter(haystack.charAt(i - 1));
            int end = i + word.length();
            boolean rightOk = end >= haystack.length() || !Character.isLetter(haystack.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            i = haystack.indexOf(word, i + 1);
        }
        return false;
    }

    private String nameOf(List<Student> students, UUID studentId) {
        return students.stream().filter(s -> s.getId().equals(studentId)).findFirst()
                .map(this::fullName)
                .orElseGet(() -> studentRepository.findById(studentId).map(this::fullName).orElse("—"));
    }

    private String fullName(Student s) {
        String first = s.getFirstName() == null ? "" : s.getFirstName();
        String last = s.getLastName() == null ? "" : s.getLastName();
        return (first + " " + last).trim();
    }

    /** Lower-cased, for matching only. */
    private static String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static java.math.BigDecimal resolvePrice(java.math.BigDecimal requested, Student student) {
        if (requested != null) {
            return requested;
        }
        if (student.getLessonPrice() != null) {
            return student.getLessonPrice();
        }
        throw ApiException.badRequest("LESSON_PRICE_REQUIRED",
                "No price given and " + student.getFirstName() + " has no default lesson price");
    }
}
