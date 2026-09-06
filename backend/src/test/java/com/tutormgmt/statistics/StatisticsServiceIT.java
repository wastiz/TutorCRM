package com.tutormgmt.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonRequest;
import com.tutormgmt.lesson.LessonService;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.student.StudentDto;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentRequest;
import com.tutormgmt.student.StudentService;
import com.tutormgmt.student.StudentStatus;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StatisticsServiceIT extends AbstractPostgresIT {

    @Autowired StatisticsService statisticsService;
    @Autowired StudentService studentService;
    @Autowired LessonService lessonService;
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

    private StudentDto student(String first, StudentStatus status) {
        return studentService.create(userId, StudentRequest.builder()
                .firstName(first).lastName("Test").email(first.toLowerCase() + "@x.ee")
                .lessonPrice(new BigDecimal("20.00")).status(status)
                .schedules(List.of()).ignoreDuplicates(true).build());
    }

    private void lesson(UUID studentId, OffsetDateTime start, LessonStatus status) {
        var l = lessonService.create(userId, new LessonRequest(studentId, start, start.plusHours(1),
                null, null, null));
        if (status != LessonStatus.PLANNED) {
            lessonService.changeStatus(userId, l.id(), status);
        }
    }

    @Test
    void countsStudentsLessonsAndEarnings() {
        UUID a = student("Anna", StudentStatus.ACTIVE).id();
        student("Boris", StudentStatus.PAUSED);
        OffsetDateTime now = OffsetDateTime.now();
        lesson(a, now.minusDays(2), LessonStatus.COMPLETED);
        lesson(a, now.minusDays(1), LessonStatus.COMPLETED);
        lesson(a, now.plusDays(1), LessonStatus.CANCELLED);
        lesson(a, now.plusDays(2), LessonStatus.NO_SHOW);
        lesson(a, now.plusDays(3), LessonStatus.PLANNED);

        StatisticsDto stats = statisticsService.overview(userId);

        assertThat(stats.students().total()).isEqualTo(2);
        assertThat(stats.students().active()).isEqualTo(1);
        assertThat(stats.students().paused()).isEqualTo(1);
        assertThat(stats.lessons().total()).isEqualTo(5);
        assertThat(stats.lessons().completed()).isEqualTo(2);
        assertThat(stats.lessons().cancelled()).isEqualTo(1);
        assertThat(stats.lessons().noShow()).isEqualTo(1);
        assertThat(stats.lessons().planned()).isEqualTo(1);
        assertThat(stats.totalEarnings()).isEqualByComparingTo("40.00");
        assertThat(stats.lessons().cancellationRate()).isEqualByComparingTo("50.0");
    }

    @Test
    void archivingRecordsWhyTheStudentLeft() {
        StudentDto anna = student("Anna", StudentStatus.ACTIVE);
        studentService.archive(userId, anna.id(), "Переехала в другой город");

        StatisticsDto stats = statisticsService.overview(userId);

        assertThat(stats.students().finished()).isEqualTo(1);
        assertThat(stats.leaveReasons()).singleElement().satisfies(r -> {
            assertThat(r.studentName()).isEqualTo("Anna Test");
            assertThat(r.reason()).isEqualTo("Переехала в другой город");
            assertThat(r.endDate()).isNotNull();
        });
    }

    @Test
    void studentsWithoutAReasonAreNotListed() {
        StudentDto anna = student("Anna", StudentStatus.ACTIVE);
        studentService.archive(userId, anna.id(), null);

        assertThat(statisticsService.overview(userId).leaveReasons()).isEmpty();
    }

    @Test
    void theBreakdownCoversTheLastTwelveMonthsEndingWithTheCurrentOne() {
        UUID a = student("Anna", StudentStatus.ACTIVE).id();
        lesson(a, OffsetDateTime.now().minusDays(1), LessonStatus.COMPLETED);

        List<StatisticsDto.MonthPointDto> months = statisticsService.overview(userId).byMonth();

        assertThat(months).hasSize(12);
        assertThat(months.get(11).month()).isEqualTo(YearMonth.now().toString());
        assertThat(months.stream().mapToLong(StatisticsDto.MonthPointDto::completed).sum())
                .isEqualTo(1);
    }

    @Test
    void anEmptyAccountReturnsZeros() {
        StatisticsDto stats = statisticsService.overview(userId);

        assertThat(stats.students().total()).isZero();
        assertThat(stats.lessons().total()).isZero();
        assertThat(stats.totalEarnings()).isEqualByComparingTo("0");
        assertThat(stats.lessons().cancellationRate()).isEqualByComparingTo("0");
        assertThat(stats.leaveReasons()).isEmpty();
    }
}
