package com.tutormgmt.student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndUserId(UUID id, UUID userId);

    List<Student> findByUserIdOrderByStudentNumberAsc(UUID userId);

    boolean existsByUserIdAndEmailIgnoreCase(UUID userId, String email);

    boolean existsByUserIdAndPhone(UUID userId, String phone);

    List<Student> findByUserIdAndEmailIgnoreCase(UUID userId, String email);

    List<Student> findByUserIdAndPhone(UUID userId, String phone);

    List<Student> findByUserIdAndFirstNameIgnoreCaseAndLastNameIgnoreCase(
            UUID userId, String firstName, String lastName);

    /** Highest numeric student_number for this tutor, or 0. Used to assign the next one. */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(student_number AS INTEGER)), 0)
            FROM student
            WHERE user_id = :userId AND student_number ~ '^[0-9]+$'
            """, nativeQuery = true)
    int maxStudentNumber(@Param("userId") UUID userId);
}
