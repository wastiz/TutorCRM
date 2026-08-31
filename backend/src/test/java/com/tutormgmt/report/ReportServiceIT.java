package com.tutormgmt.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonRequest;
import com.tutormgmt.lesson.LessonService;
import com.tutormgmt.lesson.LessonStatus;
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
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportServiceIT extends AbstractPostgresIT {

    @Autowired ReportService reportService;
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

    private UUID student(String first, String last, String email, String parent, String isikukood, BigDecimal price) {
        StudentDto s = studentService.create(userId, new StudentRequest(
                first, last, email, null, isikukood, null, null, null, "Эстонский", null, null, null,
                LessonFormat.ONLINE, price, null, null, null, parent, null, null, null, List.of(), false));
        return s.id();
    }

    private void completedLesson(UUID studentId, OffsetDateTime start, BigDecimal price, LessonStatus status) {
        var l = lessonService.create(userId, new LessonRequest(
                studentId, start, start.plusHours(1), price, null, null));
        if (status != LessonStatus.PLANNED) {
            lessonService.changeStatus(userId, l.id(), status);
        }
    }

    private static OffsetDateTime aug(int day) {
        return OffsetDateTime.of(2026, 8, day, 12, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void countsOnlyCompletedLessonsInTheMonth() {
        UUID sid = student("Nikita", "Durmanov", "nikita@x.ee", null, "39709250057", new BigDecimal("12"));
        completedLesson(sid, aug(3), new BigDecimal("12"), LessonStatus.COMPLETED);
        completedLesson(sid, aug(10), new BigDecimal("12"), LessonStatus.PLANNED);   // excluded
        completedLesson(sid, aug(17), new BigDecimal("12"), LessonStatus.CANCELLED); // excluded
        completedLesson(sid, aug(24), new BigDecimal("12"), LessonStatus.NO_SHOW);   // excluded
        completedLesson(sid, OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                new BigDecimal("12"), LessonStatus.COMPLETED);                        // next month

        MonthlyReportDto report = reportService.generate(userId, YearMonth.of(2026, 8));

        assertThat(report.title()).isEqualTo("август 26");
        assertThat(report.totalLessons()).isEqualTo(1);
        assertThat(report.totalAmount()).isEqualByComparingTo("12");
        assertThat(report.students()).singleElement().satisfies(r -> {
            assertThat(r.fullName()).isEqualTo("Nikita Durmanov");
            assertThat(r.isikukood()).isEqualTo("39709250057");
            assertThat(r.parentName()).isNull();
            assertThat(r.lessonCount()).isEqualTo(1);
            assertThat(r.total()).isEqualByComparingTo("12");
        });
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void usesLessonPriceNotCurrentStudentPrice() {
        UUID sid = student("A", "B", "a@x.ee", null, "1", new BigDecimal("12"));
        completedLesson(sid, aug(5), new BigDecimal("12"), LessonStatus.COMPLETED);
        completedLesson(sid, aug(12), new BigDecimal("12"), LessonStatus.COMPLETED);

        // student price rises later — August must stay at 12
        studentService.update(userId, sid, new StudentRequest("A", "B", "a@x.ee", null, "1", null, null,
                null, "Эстонский", null, null, null, LessonFormat.ONLINE, new BigDecimal("15"),
                null, null, null, null, null, null, null, List.of(), false));

        MonthlyReportDto report = reportService.generate(userId, YearMonth.of(2026, 8));
        assertThat(report.totalAmount()).isEqualByComparingTo("24");
        assertThat(report.students().get(0).lessonPrice()).isEqualByComparingTo("12");
    }

    @Test
    void warnsOnMultiplePricesForOneStudent() {
        UUID sid = student("Multi", "Price", null, null, "2", new BigDecimal("12"));
        completedLesson(sid, aug(4), new BigDecimal("12"), LessonStatus.COMPLETED);
        completedLesson(sid, aug(11), new BigDecimal("12"), LessonStatus.COMPLETED);
        completedLesson(sid, aug(18), new BigDecimal("15"), LessonStatus.COMPLETED);

        MonthlyReportDto report = reportService.generate(userId, YearMonth.of(2026, 8));

        assertThat(report.warnings()).anyMatch(w -> w.contains("Multi Price") && w.contains("multiple"));
        assertThat(report.students().get(0).lessonPrice()).isNull();
        assertThat(report.students().get(0).total()).isEqualByComparingTo("39");
        assertThat(report.exportable()).isTrue(); // warning, not error
    }

    @Test
    void groupsByStudentAndSortsByStudentNumber() {
        UUID s1 = student("First", "One", null, null, "1", new BigDecimal("12"));
        UUID s3 = student("Third", "Three", null, null, "3", new BigDecimal("12"));
        UUID s2 = student("Second", "Two", null, null, "2", new BigDecimal("12"));
        completedLesson(s3, aug(2), new BigDecimal("12"), LessonStatus.COMPLETED);
        completedLesson(s1, aug(3), new BigDecimal("12"), LessonStatus.COMPLETED);
        completedLesson(s1, aug(4), new BigDecimal("12"), LessonStatus.COMPLETED);
        completedLesson(s2, aug(5), new BigDecimal("12"), LessonStatus.COMPLETED);

        MonthlyReportDto report = reportService.generate(userId, YearMonth.of(2026, 8));

        assertThat(report.students()).extracting(MonthlyReportDto.StudentReportRowDto::studentNumber)
                .containsExactly("1", "2", "3");
        assertThat(report.students().get(0).lessonCount()).isEqualTo(2);
        assertThat(report.totalLessons()).isEqualTo(4);
        assertThat(report.totalAmount()).isEqualByComparingTo("48");
    }

    @Test
    void missingEmailAndParentAreEmptyNotBlocking() {
        UUID sid = student("Андрей", "NoContact", null, null, "8", new BigDecimal("18"));
        completedLesson(sid, aug(6), new BigDecimal("18"), LessonStatus.COMPLETED);
        completedLesson(sid, aug(13), new BigDecimal("18"), LessonStatus.COMPLETED);

        MonthlyReportDto report = reportService.generate(userId, YearMonth.of(2026, 8));

        MonthlyReportDto.StudentReportRowDto row = report.students().get(0);
        assertThat(row.email()).isNull();
        assertThat(row.parentName()).isNull();
        assertThat(row.total()).isEqualByComparingTo("36");
        assertThat(report.exportable()).isTrue();
    }

    @Test
    void includesLastDayOfMonthExcludesFirstOfNext() {
        UUID sid = student("Edge", "Case", null, null, "1", new BigDecimal("10"));
        completedLesson(sid, OffsetDateTime.of(2026, 8, 31, 23, 0, 0, 0, ZoneOffset.UTC),
                new BigDecimal("10"), LessonStatus.COMPLETED);
        completedLesson(sid, OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                new BigDecimal("10"), LessonStatus.COMPLETED);

        MonthlyReportDto report = reportService.generate(userId, YearMonth.of(2026, 8));
        assertThat(report.totalLessons()).isEqualTo(1);
    }

    @Test
    void emptyMonthProducesEmptyExportableReport() {
        MonthlyReportDto report = reportService.generate(userId, YearMonth.of(2026, 7));
        assertThat(report.students()).isEmpty();
        assertThat(report.totalLessons()).isZero();
        assertThat(report.totalAmount()).isEqualByComparingTo("0");
        assertThat(report.title()).isEqualTo("июль 26");
        assertThat(report.exportable()).isTrue();
    }
}
