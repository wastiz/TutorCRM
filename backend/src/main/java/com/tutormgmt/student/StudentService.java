package com.tutormgmt.student;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.student.schedule.StudentSchedule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository repository;
    private final StudentMapper mapper;

    @Transactional(readOnly = true)
    public List<StudentSummaryDto> list(UUID userId, String search, StudentStatus status) {
        String needle = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
        return repository.findByUserIdOrderByStudentNumberAsc(userId).stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> needle == null || needle.isBlank() || matches(s, needle))
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDto get(UUID userId, UUID id) {
        return mapper.toDto(require(userId, id));
    }

    @Transactional
    public StudentDto create(UUID userId, StudentRequest request) {
        if (!request.ignoreDuplicates()) {
            List<StudentSummaryDto> dups = findDuplicates(userId, new DuplicateCheckRequest(
                    request.email(), request.phone(), request.firstName(), request.lastName()));
            if (!dups.isEmpty()) {
                throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "POSSIBLE_DUPLICATE",
                        "A similar student already exists", Map.of("duplicates", dups));
            }
        }
        Student student = persistWithNumber(userId, request);
        log.info("Created student {} (#{}) for user {}", student.getId(), student.getStudentNumber(), userId);
        return mapper.toDto(student);
    }

    @Transactional
    public StudentDto update(UUID userId, UUID id, StudentRequest request) {
        Student student = require(userId, id);
        apply(student, request, false);
        student.replaceSchedules(toScheduleEntities(student.getId(), request));
        return mapper.toDto(repository.save(student));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Student student = require(userId, id);
        repository.delete(student);
        log.info("Deleted student {} for user {}", id, userId);
    }

    @Transactional
    public StudentDto archive(UUID userId, UUID id) {
        Student student = require(userId, id);
        student.setStatus(StudentStatus.FINISHED);
        if (student.getEndDate() == null) {
            student.setEndDate(LocalDate.now());
        }
        return mapper.toDto(repository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentSummaryDto> findDuplicates(UUID userId, DuplicateCheckRequest req) {
        Map<UUID, Student> found = new LinkedHashMap<>();
        if (StringUtils.hasText(req.email())) {
            repository.findByUserIdAndEmailIgnoreCase(userId, req.email().trim())
                    .forEach(s -> found.put(s.getId(), s));
        }
        if (StringUtils.hasText(req.phone())) {
            repository.findByUserIdAndPhone(userId, req.phone().trim())
                    .forEach(s -> found.put(s.getId(), s));
        }
        if (StringUtils.hasText(req.firstName()) && StringUtils.hasText(req.lastName())) {
            repository.findByUserIdAndFirstNameIgnoreCaseAndLastNameIgnoreCase(
                            userId, req.firstName().trim(), req.lastName().trim())
                    .forEach(s -> found.put(s.getId(), s));
        }
        return found.values().stream().map(mapper::toSummary).toList();
    }

    // --- internals ---

    private Student persistWithNumber(UUID userId, StudentRequest request) {
        DataIntegrityViolationException last = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            String number = String.valueOf(repository.maxStudentNumber(userId) + 1 + attempt);
            Student student = Student.create(userId, number);
            apply(student, request, true);
            student.replaceSchedules(toScheduleEntities(student.getId(), request));
            try {
                return repository.saveAndFlush(student);
            } catch (DataIntegrityViolationException ex) {
                last = ex;
                log.warn("student_number {} collided for user {}, retrying", number, userId);
            }
        }
        throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "STUDENT_NUMBER_CONFLICT",
                "Could not assign a student number, please retry", last);
    }

    private void apply(Student s, StudentRequest r, boolean isCreate) {
        s.setFirstName(trim(r.firstName()));
        s.setLastName(trim(r.lastName()));
        s.setEmail(trimToNull(r.email()));
        s.setPhone(trimToNull(r.phone()));
        s.setIsikukood(trimToNull(r.isikukood()));
        s.setAge(r.age());
        s.setGrade(r.grade());
        s.setSchool(trimToNull(r.school()));
        s.setSubject(trimToNull(r.subject()));
        s.setGoal(trimToNull(r.goal()));
        s.setLevel(trimToNull(r.level()));
        s.setNotes(trimToNull(r.notes()));
        s.setLessonFormat(r.lessonFormat());
        s.setLessonPrice(r.lessonPrice());
        s.setStartDate(r.startDate());
        s.setEndDate(r.endDate());
        s.setParentName(trimToNull(r.parentName()));
        s.setParentPhone(trimToNull(r.parentPhone()));
        s.setParentEmail(trimToNull(r.parentEmail()));
        s.setParentSecondaryPhone(trimToNull(r.parentSecondaryPhone()));
        if (r.status() != null) {
            s.setStatus(r.status());
        } else if (isCreate) {
            s.setStatus(StudentStatus.ACTIVE);
        }
    }

    private List<StudentSchedule> toScheduleEntities(UUID studentId, StudentRequest request) {
        List<StudentSchedule> out = new ArrayList<>();
        if (request.schedules() == null) {
            return out;
        }
        for (StudentRequest.ScheduleEntry e : request.schedules()) {
            if (e.dayOfWeek() == null) {
                continue;
            }
            out.add(StudentSchedule.of(studentId, e.dayOfWeek(), e.startTime(), e.endTime(), e.lessonsPerWeek()));
        }
        return out;
    }

    private Student require(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Student not found"));
    }

    private static boolean matches(Student s, String needle) {
        return contains(s.getFirstName(), needle)
                || contains(s.getLastName(), needle)
                || contains(s.getEmail(), needle)
                || contains(s.getPhone(), needle)
                || contains(s.getSubject(), needle)
                || contains(s.getStudentNumber(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String trim(String v) {
        return v == null ? null : v.trim();
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
