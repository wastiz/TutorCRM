package com.tutormgmt.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutormgmt.common.error.ApiException;
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

class LessonServiceIT extends AbstractPostgresIT {

    @Autowired LessonService lessonService;
    @Autowired StudentService studentService;
    @Autowired LessonRepository lessonRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired UserRepository userRepository;

    private UUID userId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "t@x.ee", "T", "T")).getId();
        StudentDto s = studentService.create(userId, new StudentRequest(
                "Kirill", "Tsarenkov", "k@x.ee", "1", null, 14, 9, null, "Math", null, null, null,
                LessonFormat.ONLINE, new BigDecimal("20.00"), null, null, null, null, null, null, null,
                List.of(), false));
        studentId = s.id();
    }

    private LessonRequest at(OffsetDateTime start, int minutes, BigDecimal price) {
        return new LessonRequest(studentId, start, start.plusMinutes(minutes), price, null, null);
    }

    private static OffsetDateTime dt(int day, int hour) {
        return OffsetDateTime.of(2026, 9, day, hour, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void createUsesStudentPriceWhenNoneGiven() {
        LessonDto lesson = lessonService.create(userId, at(dt(3, 17), 60, null));

        assertThat(lesson.price()).isEqualByComparingTo("20.00");
        assertThat(lesson.status()).isEqualTo(LessonStatus.PLANNED);
        assertThat(lesson.calendarSyncStatus()).isEqualTo(CalendarSyncStatus.PENDING);
        assertThat(lesson.studentName()).isEqualTo("Kirill Tsarenkov");
    }

    @Test
    void lessonPriceIsFrozenEvenIfStudentPriceChangesLater() {
        LessonDto lesson = lessonService.create(userId, at(dt(3, 17), 60, null));

        studentService.update(userId, studentId, new StudentRequest(
                "Kirill", "Tsarenkov", "k@x.ee", "1", null, null, null, null, "Math", null, null, null,
                LessonFormat.ONLINE, new BigDecimal("25.00"), null, null, null, null, null, null, null,
                List.of(), false));

        assertThat(lessonService.get(userId, lesson.id()).price()).isEqualByComparingTo("20.00");
    }

    @Test
    void statusTransitions() {
        LessonDto lesson = lessonService.create(userId, at(dt(3, 17), 60, null));

        assertThat(lessonService.changeStatus(userId, lesson.id(), LessonStatus.COMPLETED).status())
                .isEqualTo(LessonStatus.COMPLETED);
        assertThat(lessonService.changeStatus(userId, lesson.id(), LessonStatus.NO_SHOW).status())
                .isEqualTo(LessonStatus.NO_SHOW);
    }

    @Test
    void repeatGeneratesWeeklyLessonsWithSamePriceAndDuration() {
        LessonDto first = lessonService.create(userId, at(dt(3, 17), 90, new BigDecimal("22.00")));

        List<LessonDto> generated = lessonService.repeat(userId, first.id(), new RepeatLessonRequest(3, 1));

        assertThat(generated).hasSize(3);
        assertThat(generated).allSatisfy(l -> {
            assertThat(l.price()).isEqualByComparingTo("22.00");
            assertThat(java.time.Duration.between(l.startTime(), l.endTime()).toMinutes()).isEqualTo(90);
        });
        assertThat(generated.get(0).startTime()).isEqualTo(dt(10, 17));
        assertThat(generated.get(2).startTime()).isEqualTo(dt(24, 17));
        assertThat(lessonService.list(userId, studentId, null, null, null)).hasSize(4);
    }

    @Test
    void overviewSplitsUpcomingPastAndSumsCompletedEarnings() {
        OffsetDateTime now = OffsetDateTime.now();
        LessonDto pastDone = lessonService.create(userId, at(now.minusDays(10), 60, new BigDecimal("20.00")));
        lessonService.changeStatus(userId, pastDone.id(), LessonStatus.COMPLETED);
        lessonService.create(userId, at(now.minusDays(2), 60, new BigDecimal("20.00"))); // past, still planned
        lessonService.create(userId, at(now.plusDays(7), 60, new BigDecimal("20.00")));   // upcoming

        LessonStudentOverviewDto overview = lessonService.studentOverview(userId, studentId);

        assertThat(overview.upcoming()).hasSize(1);
        assertThat(overview.past()).hasSize(2);
        assertThat(overview.earningsTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> lessonService.create(userId,
                new LessonRequest(studentId, dt(3, 18), dt(3, 17), new BigDecimal("10"), null, null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cannotDeleteStudentWithLessons() {
        lessonService.create(userId, at(dt(3, 17), 60, null));

        assertThatThrownBy(() -> studentService.delete(userId, studentId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("STUDENT_HAS_LESSONS"));
    }

    @Test
    void otherUserCannotAccessLesson() {
        LessonDto lesson = lessonService.create(userId, at(dt(3, 17), 60, null));
        UUID otherUser = userRepository.save(User.create("sub-" + UUID.randomUUID(), "o@x.ee", "O", "O")).getId();

        assertThatThrownBy(() -> lessonService.get(otherUser, lesson.id())).isInstanceOf(ApiException.class);
    }
}
