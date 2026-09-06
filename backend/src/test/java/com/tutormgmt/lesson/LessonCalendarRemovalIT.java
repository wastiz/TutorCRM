package com.tutormgmt.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.student.LessonFormat;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentRequest;
import com.tutormgmt.student.StudentService;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * A lesson that did not happen must disappear from Google Calendar while the lesson row survives
 * (owner decision, 2026-09-05). Uses a fake gateway — no Google credentials in tests.
 */
@Import(LessonCalendarRemovalIT.FakeCalendarConfig.class)
class LessonCalendarRemovalIT extends AbstractPostgresIT {

    @Autowired LessonService lessonService;
    @Autowired StudentService studentService;
    @Autowired LessonRepository lessonRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired UserRepository userRepository;
    @Autowired FakeCalendarGateway calendar;

    private UUID userId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        calendar.removed.clear();
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "t@x.ee", "T", "T")).getId();
        studentId = studentService.create(userId, StudentRequest.builder()
                .firstName("Maksim").lastName("Ivanov").email("k@x.ee")
                .lessonFormat(LessonFormat.ONLINE).lessonPrice(new BigDecimal("20.00"))
                .schedules(List.of()).build()).id();
    }

    @Test
    void noShowDeletesTheCalendarEventAndKeepsTheLesson() {
        LessonDto lesson = lessonService.create(userId, request());
        assertThat(lesson.calendarSyncStatus()).isEqualTo(CalendarSyncStatus.SYNCED);

        LessonDto marked = lessonService.changeStatus(userId, lesson.id(), LessonStatus.NO_SHOW);

        assertThat(calendar.removed).containsExactly("event-1");
        assertThat(marked.googleCalendarEventId()).isNull();
        assertThat(marked.calendarSyncStatus()).isEqualTo(CalendarSyncStatus.REMOVED);
        assertThat(lessonRepository.findByIdAndUserId(lesson.id(), userId)).isPresent();
    }

    @Test
    void cancellingAlsoFreesTheSlotAndReinstatingRecreatesTheEvent() {
        LessonDto lesson = lessonService.create(userId, request());

        lessonService.changeStatus(userId, lesson.id(), LessonStatus.CANCELLED);
        assertThat(calendar.removed).hasSize(1);

        LessonDto back = lessonService.changeStatus(userId, lesson.id(), LessonStatus.PLANNED);
        assertThat(back.googleCalendarEventId()).isEqualTo("event-1");
        assertThat(back.calendarSyncStatus()).isEqualTo(CalendarSyncStatus.SYNCED);
    }

    @Test
    void syncingOneStudentPushesUpcomingLessonsAndRemovesTheOnesThatWillNotHappen() {
        LessonDto upcoming = lessonService.create(userId, request());
        LessonDto later = lessonService.create(userId, new LessonRequest(studentId,
                request().startTime().plusWeeks(1), request().endTime().plusWeeks(1), null, null, null));
        lessonService.changeStatus(userId, later.id(), LessonStatus.CANCELLED);
        calendar.removed.clear();

        CalendarSyncSummaryDto summary = lessonService.syncStudentCalendar(userId, studentId);

        assertThat(summary.enabled()).isTrue();
        assertThat(summary.considered()).isEqualTo(2);
        assertThat(summary.synced()).isEqualTo(1);
        assertThat(summary.removed()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        assertThat(lessonService.get(userId, upcoming.id()).calendarSyncStatus())
                .isEqualTo(CalendarSyncStatus.SYNCED);
    }

    private LessonRequest request() {
        // always in the future so the per-student sync picks it up
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC)
                .plusDays(3).truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        return new LessonRequest(studentId, start, start.plusHours(1), null, null, null);
    }

    /** Records what the lesson slice asks Google to do. */
    static class FakeCalendarGateway implements LessonCalendarGateway {

        final List<String> removed = new ArrayList<>();

        @Override
        public boolean enabledFor(UUID userId) {
            return true;
        }

        @Override
        public Optional<CalendarLink> sync(UUID userId, LessonSyncCommand command) {
            return Optional.of(new CalendarLink("cal-1", "event-1"));
        }

        @Override
        public void remove(UUID userId, String calendarId, String eventId) {
            removed.add(eventId);
        }
    }

    @TestConfiguration
    static class FakeCalendarConfig {
        @Bean
        @Primary
        FakeCalendarGateway fakeCalendarGateway() {
            return new FakeCalendarGateway();
        }
    }
}
