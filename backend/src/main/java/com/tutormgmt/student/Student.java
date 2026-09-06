package com.tutormgmt.student;

import com.tutormgmt.common.domain.Auditable;
import com.tutormgmt.student.schedule.StudentSchedule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A student (CLAUDE.md section 10.3, extended per Report.md section 2 with
 * {@code userId} and {@code studentNumber}).
 */
@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Owning tutor. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Sequential per tutor, assigned on creation. Stored as text (Report.md section 2). */
    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String email;

    private String phone;

    /** Estonian isikukood — stored as text, leading zeros preserved. */
    private String isikukood;

    private Integer age;

    private Integer grade;

    private String school;

    private String subject;

    @Column(length = 4000)
    private String goal;

    private String level;

    @Column(length = 4000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_format", length = 16)
    private LessonFormat lessonFormat;

    /** Current lesson price. Historical prices live on each Lesson. */
    @Column(name = "lesson_price", precision = 12, scale = 2)
    private BigDecimal lessonPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** Why the student stopped. Free text — the tutor writes it in their own words. */
    @Column(name = "leave_reason", length = 1000)
    private String leaveReason;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "parent_secondary_phone")
    private String parentSecondaryPhone;

    /** Parent's Estonian personal code, recognized by the import parser. */
    @Column(name = "parent_isikukood")
    private String parentIsikukood;

    /** Telegram username (without the "@") or a t.me link. */
    private String telegram;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "student_id", nullable = false)
    @OrderBy("dayOfWeek ASC")
    private List<StudentSchedule> schedules = new ArrayList<>();

    public static Student create(UUID userId, String studentNumber) {
        Student s = new Student();
        s.id = UUID.randomUUID();
        s.userId = userId;
        s.studentNumber = studentNumber;
        s.status = StudentStatus.ACTIVE;
        return s;
    }

    public void replaceSchedules(List<StudentSchedule> next) {
        schedules.clear();
        for (StudentSchedule s : next) {
            s.setStudentId(this.id);
            schedules.add(s);
        }
    }
}
