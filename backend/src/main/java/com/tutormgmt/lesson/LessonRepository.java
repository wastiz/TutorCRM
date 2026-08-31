package com.tutormgmt.lesson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    Optional<Lesson> findByIdAndUserId(UUID id, UUID userId);

    List<Lesson> findByUserIdAndStudentIdOrderByStartTimeDesc(UUID userId, UUID studentId);

    List<Lesson> findByUserIdOrderByStartTimeAsc(UUID userId);

    List<Lesson> findByUserIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
            UUID userId, LessonStatus status, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select l.studentId as studentId, min(l.startTime) as nextStart
            from Lesson l
            where l.userId = :userId and l.status = com.tutormgmt.lesson.LessonStatus.PLANNED
              and l.startTime >= :now
            group by l.studentId
            """)
    List<NextLessonRow> findNextPlannedPerStudent(@Param("userId") UUID userId,
                                                  @Param("now") OffsetDateTime now);

    interface NextLessonRow {
        UUID getStudentId();
        OffsetDateTime getNextStart();
    }

    long countByUserIdAndStudentId(UUID userId, UUID studentId);
}
