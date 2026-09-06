package com.tutormgmt.dashboard;

import com.tutormgmt.lesson.LessonDto;
import com.tutormgmt.lesson.LessonService;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CLAUDE.md section 38. Read-only aggregation over students + lessons. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int UPCOMING_LIMIT = 6;

    private final LessonService lessonService;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public DashboardDto summary(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime dayStart = now.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime weekStart = dayStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        OffsetDateTime monthStart = dayStart.withDayOfMonth(1);
        OffsetDateTime nextMonth = monthStart.plusMonths(1);

        List<LessonDto> month = lessonService.list(userId, null, null, monthStart, nextMonth);

        long completedThisMonth = month.stream().filter(l -> l.status() == LessonStatus.COMPLETED).count();
        BigDecimal earnings = sum(month, l -> l.status() == LessonStatus.COMPLETED);
        BigDecimal expected = sum(month,
                l -> l.status() == LessonStatus.COMPLETED || l.status() == LessonStatus.PLANNED);

        List<LessonDto> todaysLessons = lessonService.list(userId, null, null, dayStart, dayStart.plusDays(1));
        long thisWeek = lessonService.list(userId, null, null, weekStart, weekStart.plusWeeks(1)).size();

        List<LessonDto> upcoming = lessonService.list(userId, null, LessonStatus.PLANNED, now, null)
                .stream().limit(UPCOMING_LIMIT).toList();

        return new DashboardDto(
                studentRepository.countByUserIdAndStatus(userId, StudentStatus.ACTIVE),
                todaysLessons.size(),
                thisWeek,
                completedThisMonth,
                earnings,
                expected,
                todaysLessons,
                upcoming);
    }

    private static BigDecimal sum(List<LessonDto> lessons, java.util.function.Predicate<LessonDto> keep) {
        return lessons.stream().filter(keep).map(LessonDto::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
