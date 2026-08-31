package com.tutormgmt.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonRequest;
import com.tutormgmt.lesson.LessonService;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.student.LessonFormat;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentRequest;
import com.tutormgmt.student.StudentService;
import com.tutormgmt.student.StudentStatus;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardServiceIT extends AbstractPostgresIT {

    @Autowired DashboardService dashboardService;
    @Autowired LessonService lessonService;
    @Autowired StudentService studentService;
    @Autowired LessonRepository lessonRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "t@x.ee", "T", "T")).getId();
    }

    private UUID student(StudentStatus status) {
        var s = studentService.create(userId, new StudentRequest("A", "B", null, null, null, null, null,
                null, "Math", null, null, null, LessonFormat.ONLINE, new BigDecimal("20"),
                status, null, null, null, null, null, null, List.of(), true));
        return s.id();
    }

    private void lesson(UUID sid, OffsetDateTime start, LessonStatus status) {
        var l = lessonService.create(userId, new LessonRequest(sid, start, start.plusHours(1),
                new BigDecimal("20"), null, null));
        if (status != LessonStatus.PLANNED) {
            lessonService.changeStatus(userId, l.id(), status);
        }
    }

    @Test
    void aggregatesStudentsLessonsAndEarnings() {
        student(StudentStatus.ACTIVE);
        student(StudentStatus.ACTIVE);
        UUID paused = student(StudentStatus.PAUSED);

        OffsetDateTime now = OffsetDateTime.now();
        lesson(paused, now.plusHours(2), LessonStatus.PLANNED);                 // today, this week, this month
        lesson(paused, now.minusDays(3).withHour(10), LessonStatus.COMPLETED);  // earlier this week/month (if same month)
        lesson(paused, now.plusDays(20), LessonStatus.PLANNED);                 // later - maybe next month

        DashboardDto d = dashboardService.summary(userId);

        assertThat(d.activeStudents()).isEqualTo(2);
        assertThat(d.todaysLessons()).isGreaterThanOrEqualTo(1);
        assertThat(d.thisWeekLessons()).isGreaterThanOrEqualTo(1);
        assertThat(d.upcomingLessons()).isNotEmpty();
        assertThat(d.upcomingLessons().get(0).status()).isEqualTo(LessonStatus.PLANNED);
        assertThat(d.thisMonthExpectedEarnings()).isGreaterThanOrEqualTo(d.thisMonthEarnings());
    }

    @Test
    void emptyAccountReturnsZeros() {
        DashboardDto d = dashboardService.summary(userId);
        assertThat(d.activeStudents()).isZero();
        assertThat(d.todaysLessons()).isZero();
        assertThat(d.thisMonthEarnings()).isEqualByComparingTo("0");
        assertThat(d.upcomingLessons()).isEmpty();
    }
}
