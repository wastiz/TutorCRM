package com.tutormgmt.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StudentServiceIT extends AbstractPostgresIT {

    @Autowired
    StudentService service;
    @Autowired
    StudentRepository studentRepository;
    @Autowired
    UserRepository userRepository;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "a@tutor.ee", "A", "Tutor")).getId();
        otherUserId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "b@tutor.ee", "B", "Tutor")).getId();
    }

    private StudentRequest request(String first, String last, String email, String phone) {
        return new StudentRequest(first, last, email, phone, null, 14, 9, "School", "Math",
                "goal", "beginner", null, LessonFormat.ONLINE, new BigDecimal("20.00"),
                null, null, null, "Parent", "555", null, null,
                List.of(new StudentRequest.ScheduleEntry(DayOfWeek.THURSDAY, LocalTime.of(17, 0), LocalTime.of(18, 0), 1)),
                false);
    }

    @Test
    void assignsSequentialStudentNumbersPerUser() {
        StudentDto a = service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));
        StudentDto b = service.create(userId, request("Artjom", "Zimin", "z@x.ee", "222"));
        StudentDto forOther = service.create(otherUserId, request("Other", "One", "o@x.ee", "999"));

        assertThat(a.studentNumber()).isEqualTo("1");
        assertThat(b.studentNumber()).isEqualTo("2");
        assertThat(forOther.studentNumber()).isEqualTo("1");
    }

    @Test
    void persistsSchedulesAsRows() {
        StudentDto created = service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));

        StudentDto loaded = service.get(userId, created.id());
        assertThat(loaded.schedules()).singleElement().satisfies(s -> {
            assertThat(s.dayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
            assertThat(s.startTime()).isEqualTo(LocalTime.of(17, 0));
            assertThat(s.lessonsPerWeek()).isEqualTo(1);
        });
    }

    @Test
    void detectsDuplicatesByEmailPhoneAndName() {
        service.create(userId, request("Kirill", "Tsarenkov", "dup@x.ee", "5350"));

        assertThat(service.findDuplicates(userId, new DuplicateCheckRequest("dup@x.ee", null, null, null))).hasSize(1);
        assertThat(service.findDuplicates(userId, new DuplicateCheckRequest(null, "5350", null, null))).hasSize(1);
        assertThat(service.findDuplicates(userId, new DuplicateCheckRequest(null, null, "kirill", "tsarenkov"))).hasSize(1);
        assertThat(service.findDuplicates(userId, new DuplicateCheckRequest("nope@x.ee", null, null, null))).isEmpty();
    }

    @Test
    void createRejectsDuplicateUnlessIgnored() {
        service.create(userId, request("Kirill", "Tsarenkov", "dup@x.ee", "5350"));
        StudentRequest again = request("Kirill", "Tsarenkov", "dup@x.ee", "5350");

        assertThatThrownBy(() -> service.create(userId, again))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("POSSIBLE_DUPLICATE"));

        StudentRequest forced = new StudentRequest("Kirill", "Tsarenkov", "dup@x.ee", "5350", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of(), true);
        assertThat(service.create(userId, forced).studentNumber()).isEqualTo("2");
    }

    @Test
    void updateReplacesSchedulesAndFields() {
        StudentDto created = service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));

        StudentRequest update = new StudentRequest("Kirill", "Tsarenkov", "k@x.ee", "111", "39001010001",
                15, 10, "New School", "Physics", "new goal", "intermediate", "note", LessonFormat.BOTH,
                new BigDecimal("25.00"), StudentStatus.PAUSED, null, null, "Mama", "777", null, null,
                List.of(
                        new StudentRequest.ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), 2),
                        new StudentRequest.ScheduleEntry(DayOfWeek.FRIDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), 2)),
                false);

        StudentDto updated = service.update(userId, created.id(), update);

        assertThat(updated.subject()).isEqualTo("Physics");
        assertThat(updated.status()).isEqualTo(StudentStatus.PAUSED);
        assertThat(updated.lessonPrice()).isEqualByComparingTo("25.00");
        assertThat(updated.schedules()).extracting(StudentScheduleDto::dayOfWeek)
                .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(updated.studentNumber()).isEqualTo("1");
    }

    @Test
    void archiveSetsFinishedAndEndDate() {
        StudentDto created = service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));

        StudentDto archived = service.archive(userId, created.id());

        assertThat(archived.status()).isEqualTo(StudentStatus.FINISHED);
        assertThat(archived.endDate()).isNotNull();
    }

    @Test
    void deleteRemovesStudentAndSchedules() {
        StudentDto created = service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));

        service.delete(userId, created.id());

        assertThat(studentRepository.findById(created.id())).isEmpty();
    }

    @Test
    void oneUserCannotSeeAnothersStudent() {
        StudentDto created = service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));

        assertThatThrownBy(() -> service.get(otherUserId, created.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void listFiltersBySearchAndStatus() {
        service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));
        StudentDto artjom = service.create(userId, request("Artjom", "Zimin", "z@x.ee", "222"));
        service.archive(userId, artjom.id());

        assertThat(service.list(userId, "tsar", null)).hasSize(1);
        assertThat(service.list(userId, null, StudentStatus.ACTIVE)).hasSize(1);
        assertThat(service.list(userId, null, StudentStatus.FINISHED)).hasSize(1);
        assertThat(service.list(userId, null, null)).hasSize(2);
    }
}
