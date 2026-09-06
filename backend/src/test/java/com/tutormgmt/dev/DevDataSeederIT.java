package com.tutormgmt.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.lesson.Lesson;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.student.Student;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentStatus;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.UserRepository;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** The dev seeder only exists under the {@code local} profile; startup seeding is off here. */
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "app.dev.seed-on-startup=false",
        "app.dev.reset-on-startup=false",
})
class DevDataSeederIT extends AbstractPostgresIT {

    @Autowired DevDataSeeder seeder;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired LessonRepository lessonRepository;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void seedsAWorkableDemoAccount() {
        DevSeedResult result = seeder.seed(false);

        assertThat(result.seeded()).isTrue();
        assertThat(result.students()).isEqualTo(8);
        assertThat(result.lessons()).isPositive();
        assertThat(result.emailTemplates()).isPositive();

        List<Student> students = studentRepository.findByUserIdOrderByStudentNumberAsc(result.userId());
        assertThat(students).extracting(Student::getStatus)
                .contains(StudentStatus.ACTIVE, StudentStatus.PAUSED, StudentStatus.FINISHED);
        assertThat(students).filteredOn(s -> s.getLeaveReason() != null).hasSize(2);
        assertThat(students).filteredOn(s -> s.getTelegram() != null).isNotEmpty();
    }

    @Test
    void coversEveryLessonStatusAndLeavesSomethingToMarkOffToday() {
        DevSeedResult result = seeder.seed(false);
        List<Lesson> lessons = lessonRepository.findByUserIdOrderByStartTimeAsc(result.userId());

        assertThat(lessons).extracting(Lesson::getStatus).contains(
                LessonStatus.COMPLETED, LessonStatus.CANCELLED,
                LessonStatus.NO_SHOW, LessonStatus.PLANNED);

        OffsetDateTime dayStart = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
        assertThat(lessons).filteredOn(l -> !l.getStartTime().isBefore(dayStart)
                        && l.getStartTime().isBefore(dayStart.plusDays(1)))
                .isNotEmpty();
    }

    @Test
    void pricesFollowTheStudentRateAndTheLessonLength() {
        DevSeedResult result = seeder.seed(false);
        List<Lesson> lessons = lessonRepository.findByUserIdOrderByStartTimeAsc(result.userId());

        assertThat(lessons).allSatisfy(l -> assertThat(l.getPrice()).isPositive());
        // a 90-minute lesson must cost 1.5 rates, never the bare rate
        assertThat(lessons).anySatisfy(l -> {
            long minutes = ChronoUnit.MINUTES.between(l.getStartTime(), l.getEndTime());
            assertThat(minutes).isIn(60L, 90L, 120L);
        });
    }

    @Test
    void runningAgainKeepsWhateverYouClickedTogether() {
        DevSeedResult first = seeder.seed(false);
        long lessonsBefore = lessonRepository.count();

        DevSeedResult second = seeder.seed(false);

        assertThat(second.seeded()).isFalse();
        assertThat(second.userId()).isEqualTo(first.userId());
        assertThat(lessonRepository.count()).isEqualTo(lessonsBefore);
    }

    @Test
    void resetReplacesTheDemoDataButKeepsTheSameTutor() {
        DevSeedResult first = seeder.seed(false);
        UUID studentToBeReplaced = studentRepository
                .findByUserIdOrderByStudentNumberAsc(first.userId()).get(0).getId();

        DevSeedResult again = seeder.seed(true);

        assertThat(again.seeded()).isTrue();
        assertThat(again.userId()).isEqualTo(first.userId());
        assertThat(again.students()).isEqualTo(8);
        assertThat(studentRepository.findById(studentToBeReplaced)).isEmpty();
    }
}
