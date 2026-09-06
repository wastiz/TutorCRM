package com.tutormgmt.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.lesson.LessonRepository;
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
    @Autowired
    LessonRepository lessonRepository;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "a@tutor.ee", "A", "Tutor")).getId();
        otherUserId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "b@tutor.ee", "B", "Tutor")).getId();
    }

    private StudentRequest request(String first, String last, String email, String phone) {
        return StudentRequest.builder()
                .firstName(first).lastName(last).email(email).phone(phone)
                .age(14).grade(9).school("School").subject("Math").goal("goal").level("beginner")
                .lessonFormat(LessonFormat.ONLINE).lessonPrice(new BigDecimal("20.00"))
                .parentName("Parent").parentPhone("555")
                .schedules(List.of(new StudentRequest.ScheduleEntry(
                        DayOfWeek.THURSDAY, LocalTime.of(17, 0), LocalTime.of(18, 0), 1)))
                .build();
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

        StudentRequest forced = StudentRequest.builder()
                .firstName("Kirill").lastName("Tsarenkov").email("dup@x.ee").phone("5350")
                .schedules(List.of()).ignoreDuplicates(true).build();
        assertThat(service.create(userId, forced).studentNumber()).isEqualTo("2");
    }

    @Test
    void telegramIsStoredAsABareUsernameWhateverTheTutorPastes() {
        StudentDto fromLink = service.create(userId, StudentRequest.builder()
                .firstName("A").lastName("B").email("a1@x.ee").telegram("https://t.me/anna_tutor")
                .schedules(List.of()).ignoreDuplicates(true).build());
        StudentDto fromHandle = service.create(userId, StudentRequest.builder()
                .firstName("C").lastName("D").email("c@x.ee").telegram("@anna_tutor")
                .schedules(List.of()).ignoreDuplicates(true).build());
        StudentDto empty = service.create(userId, StudentRequest.builder()
                .firstName("E").lastName("F").email("e@x.ee").telegram("  ")
                .schedules(List.of()).ignoreDuplicates(true).build());

        assertThat(fromLink.telegram()).isEqualTo("anna_tutor");
        assertThat(fromHandle.telegram()).isEqualTo("anna_tutor");
        assertThat(empty.telegram()).isNull();
    }

    @Test
    void updateReplacesSchedulesAndFields() {
        StudentDto created = service.create(userId, request("Kirill", "Tsarenkov", "k@x.ee", "111"));

        StudentRequest update = StudentRequest.builder()
                .firstName("Kirill").lastName("Tsarenkov").email("k@x.ee").phone("111")
                .isikukood("39001010001").age(15).grade(10).school("New School").subject("Physics")
                .goal("new goal").level("intermediate").notes("note").lessonFormat(LessonFormat.BOTH)
                .lessonPrice(new BigDecimal("25.00")).status(StudentStatus.PAUSED)
                .parentName("Mama").parentPhone("777")
                .schedules(List.of(
                        new StudentRequest.ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), 2),
                        new StudentRequest.ScheduleEntry(DayOfWeek.FRIDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), 2)))
                .build();

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

        StudentDto archived = service.archive(userId, created.id(), null);

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
        service.archive(userId, artjom.id(), null);

        assertThat(service.list(userId, "tsar", null)).hasSize(1);
        assertThat(service.list(userId, null, StudentStatus.ACTIVE)).hasSize(1);
        assertThat(service.list(userId, null, StudentStatus.FINISHED)).hasSize(1);
        assertThat(service.list(userId, null, null)).hasSize(2);
    }
}
