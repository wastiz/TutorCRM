package com.tutormgmt.lesson;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.student.Student;
import com.tutormgmt.student.StudentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository repository;
    private final StudentRepository studentRepository;
    private final LessonMapper mapper;

    @Transactional(readOnly = true)
    public List<LessonDto> list(UUID userId, UUID studentId, LessonStatus status,
                                OffsetDateTime from, OffsetDateTime to) {
        List<Lesson> filtered = repository.findByUserIdOrderByStartTimeAsc(userId).stream()
                .filter(l -> studentId == null || l.getStudentId().equals(studentId))
                .filter(l -> status == null || l.getStatus() == status)
                .filter(l -> from == null || !l.getStartTime().isBefore(from))
                .filter(l -> to == null || l.getStartTime().isBefore(to))
                .toList();
        return withStudentNames(filtered);
    }

    @Transactional(readOnly = true)
    public LessonDto get(UUID userId, UUID id) {
        return withStudentNames(List.of(require(userId, id))).get(0);
    }

    @Transactional
    public LessonDto create(UUID userId, LessonRequest request) {
        Student student = requireStudent(userId, request.studentId());
        validateTimes(request.startTime(), request.endTime());

        Lesson lesson = Lesson.create(userId, student.getId());
        lesson.setStartTime(request.startTime());
        lesson.setEndTime(request.endTime());
        lesson.setPrice(resolvePrice(request.price(), student));
        lesson.setStatus(request.status() == null ? LessonStatus.PLANNED : request.status());
        lesson.setNotes(trimToNull(request.notes()));
        Lesson saved = repository.save(lesson);
        log.info("Created lesson {} for student {} ({})", saved.getId(), student.getId(), saved.getStartTime());
        return withStudentNames(List.of(saved)).get(0);
    }

    @Transactional
    public LessonDto update(UUID userId, UUID id, LessonRequest request) {
        Lesson lesson = require(userId, id);
        Student student = requireStudent(userId, request.studentId());
        validateTimes(request.startTime(), request.endTime());

        lesson.setStudentId(student.getId());
        lesson.setStartTime(request.startTime());
        lesson.setEndTime(request.endTime());
        if (request.price() != null) {
            lesson.setPrice(request.price());
        }
        if (request.status() != null) {
            lesson.setStatus(request.status());
        }
        lesson.setNotes(trimToNull(request.notes()));
        if (lesson.getCalendarSyncStatus() == CalendarSyncStatus.SYNCED) {
            lesson.setCalendarSyncStatus(CalendarSyncStatus.PENDING); // needs re-push after an edit
        }
        return withStudentNames(List.of(repository.save(lesson))).get(0);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(require(userId, id));
    }

    @Transactional
    public LessonDto changeStatus(UUID userId, UUID id, LessonStatus status) {
        Lesson lesson = require(userId, id);
        lesson.setStatus(status);
        return withStudentNames(List.of(repository.save(lesson))).get(0);
    }

    @Transactional
    public List<LessonDto> repeat(UUID userId, UUID id, RepeatLessonRequest request) {
        Lesson source = require(userId, id);
        long durationSeconds = ChronoUnit.SECONDS.between(source.getStartTime(), source.getEndTime());
        int step = request.intervalWeeksOrDefault();

        List<Lesson> created = new ArrayList<>();
        for (int i = 1; i <= request.occurrences(); i++) {
            OffsetDateTime start = source.getStartTime().plusWeeks((long) step * i);
            Lesson copy = Lesson.create(userId, source.getStudentId());
            copy.setStartTime(start);
            copy.setEndTime(start.plusSeconds(durationSeconds));
            copy.setPrice(source.getPrice());
            copy.setStatus(LessonStatus.PLANNED);
            copy.setNotes(source.getNotes());
            created.add(copy);
        }
        List<Lesson> saved = repository.saveAll(created);
        log.info("Generated {} recurring lessons from {}", saved.size(), id);
        return withStudentNames(saved);
    }

    @Transactional(readOnly = true)
    public LessonStudentOverviewDto studentOverview(UUID userId, UUID studentId) {
        requireStudent(userId, studentId);
        List<Lesson> all = repository.findByUserIdAndStudentIdOrderByStartTimeDesc(userId, studentId);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime monthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime nextMonth = monthStart.plusMonths(1);

        List<LessonDto> upcoming = withStudentNames(all.stream()
                .filter(l -> l.getStartTime().isAfter(now) && l.getStatus() == LessonStatus.PLANNED)
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .toList());
        List<LessonDto> past = withStudentNames(all.stream()
                .filter(l -> !l.getStartTime().isAfter(now) || l.getStatus() != LessonStatus.PLANNED)
                .toList());

        List<Lesson> completed = all.stream().filter(l -> l.getStatus() == LessonStatus.COMPLETED).toList();
        BigDecimal earningsTotal = completed.stream()
                .map(Lesson::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Lesson> completedThisMonth = completed.stream()
                .filter(l -> !l.getStartTime().isBefore(monthStart) && l.getStartTime().isBefore(nextMonth))
                .toList();
        BigDecimal earningsThisMonth = completedThisMonth.stream()
                .map(Lesson::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LessonStudentOverviewDto(upcoming, past, completedThisMonth.size(),
                earningsThisMonth, earningsTotal);
    }

    // --- internals ---

    private List<LessonDto> withStudentNames(List<Lesson> lessons) {
        if (lessons.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> names = studentRepository.findAllById(
                        lessons.stream().map(Lesson::getStudentId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Student::getId,
                        s -> (s.getFirstName() + " " + s.getLastName()).trim()));
        return lessons.stream()
                .map(l -> {
                    LessonDto dto = mapper.toDto(l);
                    return new LessonDto(dto.id(), dto.studentId(), names.get(l.getStudentId()),
                            dto.startTime(), dto.endTime(), dto.price(), dto.status(), dto.notes(),
                            dto.googleCalendarId(), dto.googleCalendarEventId(), dto.calendarSyncStatus(),
                            dto.createdAt(), dto.updatedAt());
                })
                .toList();
    }

    private Lesson require(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("LESSON_NOT_FOUND", "Lesson not found"));
    }

    private Student requireStudent(UUID userId, UUID studentId) {
        return studentRepository.findByIdAndUserId(studentId, userId)
                .orElseThrow(() -> ApiException.badRequest("STUDENT_NOT_FOUND", "Student not found"));
    }

    private static void validateTimes(OffsetDateTime start, OffsetDateTime end) {
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("INVALID_LESSON_TIME", "Lesson end must be after its start");
        }
        if (ChronoUnit.HOURS.between(start, end) > 12) {
            throw ApiException.badRequest("INVALID_LESSON_TIME", "Lesson is unrealistically long");
        }
    }

    private static BigDecimal resolvePrice(BigDecimal requested, Student student) {
        if (requested != null) {
            return requested;
        }
        if (student.getLessonPrice() != null) {
            return student.getLessonPrice();
        }
        throw ApiException.badRequest("LESSON_PRICE_REQUIRED",
                "No price given and the student has no default lesson price");
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
