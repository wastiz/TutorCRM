package com.tutormgmt.report;

import com.tutormgmt.lesson.Lesson;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.report.MonthlyReportDto.StudentReportRowDto;
import com.tutormgmt.student.Student;
import com.tutormgmt.student.StudentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the monthly payout report purely from {@code Lesson} rows (Report.md sections 8-11, 20-24).
 * Google Sheets is a downstream export target only — never a data source.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public MonthlyReportDto generate(UUID userId, YearMonth month) {
        OffsetDateTime start = month.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = month.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        String title = RussianMonths.title(month);

        List<Lesson> completed = lessonRepository
                .findByUserIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                        userId, LessonStatus.COMPLETED, start, end);

        Map<UUID, List<Lesson>> byStudent = new LinkedHashMap<>();
        for (Lesson l : completed) {
            byStudent.computeIfAbsent(l.getStudentId(), k -> new ArrayList<>()).add(l);
        }

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<StudentReportRowDto> rows = new ArrayList<>();

        for (Map.Entry<UUID, List<Lesson>> e : byStudent.entrySet()) {
            Student s = studentRepository.findByIdAndUserId(e.getKey(), userId).orElse(null);
            List<Lesson> lessons = e.getValue();

            BigDecimal total = lessons.stream()
                    .map(Lesson::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<BigDecimal> distinctPrices = lessons.stream()
                    .map(Lesson::getPrice).distinct().toList();
            BigDecimal rowPrice = distinctPrices.size() == 1 ? distinctPrices.get(0) : null;

            if (s == null) {
                errors.add("A completed lesson references a student that no longer exists");
                continue;
            }
            String name = (nz(s.getFirstName()) + " " + nz(s.getLastName())).trim();
            if (rowPrice == null) {
                warnings.add("%s has multiple lesson prices in %s — resolve before export".formatted(name, title));
            }
            rows.add(new StudentReportRowDto(
                    name,
                    s.getStudentNumber(),
                    s.getEmail(),
                    s.getParentName(),
                    s.getIsikukood(),
                    s.getSubject(),
                    lessons.size(),
                    rowPrice,
                    total));
        }

        rows.sort(Comparator
                .comparingInt((StudentReportRowDto r) -> numeric(r.studentNumber()))
                .thenComparing(r -> nz(r.studentNumber())));

        int totalLessons = rows.stream().mapToInt(StudentReportRowDto::lessonCount).sum();
        BigDecimal totalAmount = rows.stream()
                .map(StudentReportRowDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Report {} for user {}: {} students, {} lessons, total {}",
                title, userId, rows.size(), totalLessons, totalAmount);
        return new MonthlyReportDto(month, title, rows, totalLessons, totalAmount, warnings, errors);
    }

    private static int numeric(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
