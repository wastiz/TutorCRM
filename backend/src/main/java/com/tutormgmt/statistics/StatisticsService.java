package com.tutormgmt.statistics;

import com.tutormgmt.lesson.Lesson;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.student.Student;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentStatus;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Read-only aggregation for the statistics page (owner request, 2026-09-05):
 * how many students there are, how many lessons happened, how many fell through,
 * and why students left.
 *
 * <p>Like the monthly report, it is computed from {@code Lesson} rows rather than stored,
 * so it can never drift from the underlying data.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /** How much history the per-month breakdown covers. */
    private static final int MONTHS = 12;

    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public StatisticsDto overview(UUID userId) {
        List<Student> students = studentRepository.findByUserIdOrderByStudentNumberAsc(userId);
        List<Lesson> lessons = lessonRepository.findByUserIdOrderByStartTimeAsc(userId);

        StatisticsDto.StudentCounts studentCounts = new StatisticsDto.StudentCounts(
                students.size(),
                count(students, s -> s.getStatus() == StudentStatus.ACTIVE),
                count(students, s -> s.getStatus() == StudentStatus.PAUSED),
                count(students, s -> s.getStatus() == StudentStatus.FINISHED));

        StatisticsDto.LessonCounts lessonCounts = new StatisticsDto.LessonCounts(
                lessons.size(),
                count(lessons, l -> l.getStatus() == LessonStatus.COMPLETED),
                count(lessons, l -> l.getStatus() == LessonStatus.CANCELLED),
                count(lessons, l -> l.getStatus() == LessonStatus.NO_SHOW),
                count(lessons, l -> l.getStatus() == LessonStatus.PLANNED));

        BigDecimal earnings = lessons.stream()
                .filter(l -> l.getStatus() == LessonStatus.COMPLETED)
                .map(Lesson::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatisticsDto(studentCounts, lessonCounts, earnings,
                leaveReasons(students), byMonth(lessons));
    }

    /** Students who stopped and left a note about why, most recent first. */
    private static List<StatisticsDto.LeaveReasonDto> leaveReasons(List<Student> students) {
        return students.stream()
                .filter(s -> StringUtils.hasText(s.getLeaveReason()))
                .sorted(Comparator.comparing(Student::getEndDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(s -> new StatisticsDto.LeaveReasonDto(s.getId(),
                        (s.getFirstName() + " " + s.getLastName()).trim(),
                        s.getEndDate(), s.getLeaveReason()))
                .toList();
    }

    /** The last {@value #MONTHS} months including the current one, oldest first. */
    private static List<StatisticsDto.MonthPointDto> byMonth(List<Lesson> lessons) {
        Map<YearMonth, List<Lesson>> grouped = lessons.stream()
                .collect(Collectors.groupingBy(l -> YearMonth.from(l.getStartTime())));

        List<StatisticsDto.MonthPointDto> out = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            List<Lesson> inMonth = grouped.getOrDefault(month, List.of());
            BigDecimal earned = inMonth.stream()
                    .filter(l -> l.getStatus() == LessonStatus.COMPLETED)
                    .map(Lesson::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new StatisticsDto.MonthPointDto(month.toString(),
                    count(inMonth, l -> l.getStatus() == LessonStatus.COMPLETED),
                    count(inMonth, l -> l.getStatus() == LessonStatus.CANCELLED),
                    count(inMonth, l -> l.getStatus() == LessonStatus.NO_SHOW),
                    earned));
        }
        return out;
    }

    private static <T> long count(List<T> items, Predicate<T> keep) {
        return items.stream().filter(keep).count();
    }
}
