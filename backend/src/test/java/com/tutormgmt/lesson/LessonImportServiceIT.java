package com.tutormgmt.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.student.LessonFormat;
import com.tutormgmt.student.StudentDto;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentRequest;
import com.tutormgmt.student.StudentService;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Uses an in-test {@link CalendarEventSource} so no real Google call happens.
 */
class LessonImportServiceIT extends AbstractPostgresIT {

    @TestConfiguration
    static class FakeSourceConfig {
        static final java.util.List<CalendarEventSource.ExternalEvent> EVENTS =
                new java.util.concurrent.CopyOnWriteArrayList<>();
        static final java.util.List<String> LINKED = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Bean
        @Primary
        CalendarEventSource fakeCalendarEventSource() {
            return new CalendarEventSource() {
                @Override public boolean enabledFor(UUID userId) { return true; }
                @Override public String targetCalendarId(UUID userId) { return "cal-1"; }
                @Override public List<ExternalEvent> fetch(UUID u, OffsetDateTime f, OffsetDateTime t) {
                    return List.copyOf(EVENTS);
                }
                @Override public void link(UUID u, String cal, String eventId, UUID lessonId) {
                    LINKED.add(eventId);
                }
            };
        }
    }

    @Autowired LessonImportService importService;
    @Autowired LessonService lessonService;
    @Autowired StudentService studentService;
    @Autowired LessonRepository lessonRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired UserRepository userRepository;

    private UUID userId;
    private UUID kirillId;

    @BeforeEach
    void setUp() {
        FakeSourceConfig.EVENTS.clear();
        FakeSourceConfig.LINKED.clear();
        lessonRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "t@x.ee", "T", "T")).getId();
        StudentDto k = studentService.create(userId, new StudentRequest("Kirill", "Tsarenkov",
                "kirill@x.ee", null, null, null, null, null, "Эстонский", null, null, null,
                LessonFormat.ONLINE, new BigDecimal("20"), null, null, null, null, null, null, null,
                List.of(), false));
        kirillId = k.id();
    }

    private static CalendarEventSource.ExternalEvent event(String id, String summary, OffsetDateTime start) {
        return new CalendarEventSource.ExternalEvent(id, "cal-1", summary, "notes",
                start, start.plusHours(1), null, false, List.of());
    }

    private static OffsetDateTime at(int day, int hour) {
        return OffsetDateTime.of(2026, 9, day, hour, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void previewGuessesStudentFromEventTitle() {
        FakeSourceConfig.EVENTS.add(event("e1", "Урок Kirill", at(3, 17)));
        FakeSourceConfig.EVENTS.add(event("e2", "Dentist", at(4, 9)));

        var preview = importService.preview(userId, at(1, 0), at(30, 0));

        assertThat(preview.enabled()).isTrue();
        assertThat(preview.newEvents()).hasSize(2);
        assertThat(preview.newEvents())
                .filteredOn(n -> n.eventId().equals("e1")).singleElement()
                .satisfies(n -> {
                    assertThat(n.suggestedStudentId()).isEqualTo(kirillId);
                    assertThat(n.suggestedPrice()).isEqualByComparingTo("20");
                });
        assertThat(preview.newEvents())
                .filteredOn(n -> n.eventId().equals("e2")).singleElement()
                .satisfies(n -> assertThat(n.suggestedStudentId()).isNull());
    }

    @Test
    void importCreatesLinkedLessonsAndTagsTheEvent() {
        FakeSourceConfig.EVENTS.add(event("e1", "Kirill Tsarenkov", at(3, 17)));

        var result = importService.importSelected(userId, new GoogleImportDto.ImportRequest(
                at(1, 0), at(30, 0),
                List.of(new GoogleImportDto.ImportRequest.Item("e1", kirillId, null)), false));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(FakeSourceConfig.LINKED).containsExactly("e1");

        var lessons = lessonService.list(userId, kirillId, null, null, null);
        assertThat(lessons).singleElement().satisfies(l -> {
            assertThat(l.googleCalendarEventId()).isEqualTo("e1");
            assertThat(l.calendarSyncStatus()).isEqualTo(CalendarSyncStatus.SYNCED);
            assertThat(l.price()).isEqualByComparingTo("20");
            assertThat(l.startTime()).isEqualTo(at(3, 17));
        });
    }

    @Test
    void doesNotReImportAnAlreadyLinkedEvent() {
        FakeSourceConfig.EVENTS.add(event("e1", "Kirill", at(3, 17)));
        importService.importSelected(userId, new GoogleImportDto.ImportRequest(at(1, 0), at(30, 0),
                List.of(new GoogleImportDto.ImportRequest.Item("e1", kirillId, null)), false));

        var preview = importService.preview(userId, at(1, 0), at(30, 0));
        assertThat(preview.newEvents()).isEmpty();
        assertThat(preview.alreadyLinked()).isEqualTo(1);

        var again = importService.importSelected(userId, new GoogleImportDto.ImportRequest(at(1, 0), at(30, 0),
                List.of(new GoogleImportDto.ImportRequest.Item("e1", kirillId, null)), false));
        assertThat(again.imported()).isZero();
    }

    @Test
    void detectsAndUpdatesMovedLinkedEvent() {
        FakeSourceConfig.EVENTS.add(event("e1", "Kirill", at(3, 17)));
        importService.importSelected(userId, new GoogleImportDto.ImportRequest(at(1, 0), at(30, 0),
                List.of(new GoogleImportDto.ImportRequest.Item("e1", kirillId, null)), false));

        // event moved in Google
        FakeSourceConfig.EVENTS.clear();
        FakeSourceConfig.EVENTS.add(event("e1", "Kirill", at(5, 19)));

        var preview = importService.preview(userId, at(1, 0), at(30, 0));
        assertThat(preview.movedLessons()).singleElement()
                .satisfies(m -> assertThat(m.newStart()).isEqualTo(at(5, 19)));

        var result = importService.importSelected(userId, new GoogleImportDto.ImportRequest(at(1, 0), at(30, 0),
                List.of(), true));
        assertThat(result.updated()).isEqualTo(1);
        assertThat(lessonService.list(userId, kirillId, null, null, null).get(0).startTime())
                .isEqualTo(at(5, 19));
    }
}
