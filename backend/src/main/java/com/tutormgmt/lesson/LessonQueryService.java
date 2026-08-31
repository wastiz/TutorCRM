package com.tutormgmt.lesson;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only lesson projections consumed by other slices (e.g. the students list needs
 * each student's next lesson). Kept separate from {@link LessonService} so there is no
 * bean cycle with the student slice.
 */
@Service
@RequiredArgsConstructor
public class LessonQueryService {

    private final LessonRepository repository;

    @Transactional(readOnly = true)
    public boolean studentHasLessons(UUID userId, UUID studentId) {
        return repository.countByUserIdAndStudentId(userId, studentId) > 0;
    }

    @Transactional(readOnly = true)
    public Map<UUID, OffsetDateTime> nextPlannedLessonStartByStudent(UUID userId) {
        Map<UUID, OffsetDateTime> out = new LinkedHashMap<>();
        for (LessonRepository.NextLessonRow row :
                repository.findNextPlannedPerStudent(userId, OffsetDateTime.now())) {
            out.put(row.getStudentId(), row.getNextStart());
        }
        return out;
    }
}
