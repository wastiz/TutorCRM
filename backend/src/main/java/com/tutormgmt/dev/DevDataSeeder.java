package com.tutormgmt.dev;

import com.tutormgmt.email.EmailService;
import com.tutormgmt.lesson.CalendarSyncStatus;
import com.tutormgmt.lesson.Lesson;
import com.tutormgmt.lesson.LessonPricing;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.student.LessonFormat;
import com.tutormgmt.student.StudentDto;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentRequest;
import com.tutormgmt.student.StudentService;
import com.tutormgmt.student.StudentStatus;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills a local database with a believable tutor's month so every screen has something to show:
 * students in all three statuses (two of them archived with a reason), lessons of every status and
 * every supported length, a few lessons today to mark off, and a price change mid-history so the
 * monthly report's multi-price warning fires.
 *
 * <p>All data hangs off one demo tutor identified by
 * {@link DevProperties#DEV_GOOGLE_SUBJECT}; nothing else in the database is touched, so a reseed
 * can never remove data belonging to a real Google account.
 */
@Slf4j
@Profile("local")
@Service
@RequiredArgsConstructor
public class DevDataSeeder {

    /** How far back the generated history goes. */
    private static final int HISTORY_WEEKS = 18;
    /** How far ahead lessons are planned. */
    private static final int FUTURE_WEEKS = 3;

    private final DevProperties props;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final LessonRepository lessonRepository;
    private final EmailService emailService;

    /**
     * @param reset drop the demo tutor's existing students and lessons first
     * @return what was created (or what already existed)
     */
    @Transactional
    public DevSeedResult seed(boolean reset) {
        User tutor = demoTutor();
        if (reset) {
            wipe(tutor.getId());
        } else if (!studentRepository.findByUserIdOrderByStudentNumberAsc(tutor.getId()).isEmpty()) {
            long students = studentRepository.findByUserIdOrderByStudentNumberAsc(tutor.getId()).size();
            long lessons = lessonRepository.findByUserIdOrderByStartTimeAsc(tutor.getId()).size();
            log.info("Dev seed skipped — demo tutor already has {} student(s)", students);
            return new DevSeedResult(false, tutor.getId(), tutor.getEmail(), students, lessons, 0);
        }

        List<StudentDto> students = createStudents(tutor.getId());
        int lessons = createLessons(tutor.getId(), students);
        // materialises the starter templates so the Settings page is not empty either
        int templates = emailService.list(tutor.getId()).size();

        log.info("Dev seed: {} students, {} lessons, {} e-mail templates for {}",
                students.size(), lessons, templates, tutor.getEmail());
        return new DevSeedResult(true, tutor.getId(), tutor.getEmail(), students.size(), lessons, templates);
    }

    /** The tutor every seeded row belongs to; created on first use. */
    @Transactional
    public User demoTutor() {
        return userRepository.findByGoogleSubject(DevProperties.DEV_GOOGLE_SUBJECT)
                .orElseGet(() -> userRepository.save(User.create(
                        DevProperties.DEV_GOOGLE_SUBJECT, props.email(),
                        props.firstName(), props.lastName())));
    }

    private void wipe(UUID userId) {
        lessonRepository.deleteAll(lessonRepository.findByUserIdOrderByStartTimeAsc(userId));
        lessonRepository.flush();
        studentRepository.deleteAll(studentRepository.findByUserIdOrderByStudentNumberAsc(userId));
        studentRepository.flush();
        log.info("Dev seed: wiped the demo tutor's previous data");
    }

    // --- students ---

    private List<StudentDto> createStudents(UUID userId) {
        LocalDate today = LocalDate.now();
        List<StudentRequest> requests = List.of(
                StudentRequest.builder()
                        .firstName("Maksim").lastName("Ivanov").email("maksim.ivanov@example.com")
                        .phone("5555 0101").isikukood("51109180007").age(14).grade(9).school("Tallinna Tõnismäe")
                        .subject("Эстонский язык").level("Слабый")
                        .goal("Слабый, особенно речь, нужна подготовка к экзамену")
                        .lessonFormat(LessonFormat.BOTH).lessonPrice(new BigDecimal("25.00"))
                        .status(StudentStatus.ACTIVE).startDate(today.minusWeeks(HISTORY_WEEKS))
                        .telegram("maksim_ts")
                        .parentName("Natalia Ivanova").parentPhone("5555 0303")
                        .parentEmail("natalia.ivanova@example.com").parentIsikukood("47808060003")
                        .schedules(List.of(new StudentRequest.ScheduleEntry(
                                DayOfWeek.THURSDAY, LocalTime.of(17, 0), LocalTime.of(20, 0), 2)))
                        .ignoreDuplicates(true).build(),

                StudentRequest.builder()
                        .firstName("Denis").lastName("Sokolov").email("denis.sokolov@example.com")
                        .phone("5817 0531").isikukood("50712210004").age(18).grade(12).school("TTG")
                        .subject("Математика").level("Слабый")
                        .goal("Подготовка к экзаменам в 12 классе")
                        .lessonFormat(LessonFormat.ONLINE).lessonPrice(new BigDecimal("25.00"))
                        .status(StudentStatus.ACTIVE).startDate(today.minusWeeks(HISTORY_WEEKS - 2))
                        .telegram("denis_z")
                        .parentName("Anna Sokolova").parentEmail("anna.sokolova@example.com")
                        .parentIsikukood("38204110003")
                        .schedules(List.of(
                                new StudentRequest.ScheduleEntry(DayOfWeek.FRIDAY, LocalTime.of(16, 0),
                                        LocalTime.of(17, 30), 1),
                                new StudentRequest.ScheduleEntry(DayOfWeek.SUNDAY, LocalTime.of(12, 0),
                                        LocalTime.of(13, 0), 1)))
                        .ignoreDuplicates(true).build(),

                StudentRequest.builder()
                        .firstName("Maria").lastName("Kask").email("maria.kask@example.com")
                        .phone("+372 5566 7788").isikukood("60903140005").age(16).grade(10)
                        .school("Tallinna Reaalkool").subject("Физика").level("Средний")
                        .goal("Подтянуть механику перед контрольными")
                        .lessonFormat(LessonFormat.OFFLINE).lessonPrice(new BigDecimal("30.00"))
                        .status(StudentStatus.ACTIVE).startDate(today.minusWeeks(10))
                        .telegram("maria_kask")
                        .parentName("Anne Kask").parentPhone("5566 1122")
                        .schedules(List.of(new StudentRequest.ScheduleEntry(
                                DayOfWeek.TUESDAY, LocalTime.of(18, 30), LocalTime.of(20, 0), 1)))
                        .ignoreDuplicates(true).build(),

                StudentRequest.builder()
                        .firstName("Daniil").lastName("Orlov").email("daniil.orlov@example.com")
                        .phone("5901 2233").isikukood("61205070000").age(13).grade(7)
                        .school("Lasnamäe Gümnaasium").subject("Английский язык").level("Начальный")
                        .goal("Разговорная практика, подтянуть грамматику")
                        .lessonFormat(LessonFormat.ONLINE).lessonPrice(new BigDecimal("20.00"))
                        .status(StudentStatus.ACTIVE).startDate(today.minusWeeks(6))
                        .parentName("Olga Orlova").parentPhone("5901 4455")
                        .parentEmail("olga.orlova@example.com")
                        .schedules(List.of(new StudentRequest.ScheduleEntry(
                                DayOfWeek.WEDNESDAY, LocalTime.of(15, 0), LocalTime.of(16, 0), 1)))
                        .ignoreDuplicates(true).build(),

                // deliberately minimal: only the three required fields — tests the optional-field rules
                StudentRequest.builder()
                        .firstName("Sofia").lastName("Petrova").email("sofia.petrova@example.com")
                        .status(StudentStatus.ACTIVE).lessonPrice(new BigDecimal("27.00"))
                        .startDate(today.minusWeeks(3))
                        .ignoreDuplicates(true).build(),

                StudentRequest.builder()
                        .firstName("Mihkel").lastName("Saar").email("mihkel.saar@example.com")
                        .phone("5432 1010").isikukood("51001300008").age(15).grade(9)
                        .school("Pelgulinna Gümnaasium").subject("Эстонский язык").level("Средний")
                        .goal("Пауза до конца учебного года — семья в отъезде")
                        .lessonFormat(LessonFormat.BOTH).lessonPrice(new BigDecimal("25.00"))
                        .status(StudentStatus.PAUSED).startDate(today.minusWeeks(14))
                        .telegram("mihkel_saar")
                        .parentName("Katrin Saar").parentEmail("katrin.saar@example.com")
                        .schedules(List.of(new StudentRequest.ScheduleEntry(
                                DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(18, 0), 1)))
                        .ignoreDuplicates(true).build(),

                StudentRequest.builder()
                        .firstName("Jelena").lastName("Volkova").email("jelena.volkova@example.com")
                        .phone("5678 4321").isikukood("60811020009").age(17).grade(11)
                        .school("Kadrioru Saksa Gümnaasium").subject("Математика").level("Хороший")
                        .goal("Готовились к экзамену — сдали")
                        .lessonFormat(LessonFormat.ONLINE).lessonPrice(new BigDecimal("25.00"))
                        .status(StudentStatus.FINISHED)
                        .startDate(today.minusWeeks(HISTORY_WEEKS)).endDate(today.minusWeeks(5))
                        .leaveReason("Сдала экзамен на нужный балл, занятия больше не нужны")
                        .parentName("Irina Volkova").parentEmail("irina.volkova@example.com")
                        .ignoreDuplicates(true).build(),

                StudentRequest.builder()
                        .firstName("Roman").lastName("Ilves").email("roman.ilves@example.com")
                        .phone("5099 8877").isikukood("51306090003").age(12).grade(6)
                        .school("Mustamäe Gümnaasium").subject("Английский язык").level("Начальный")
                        .lessonFormat(LessonFormat.OFFLINE).lessonPrice(new BigDecimal("22.00"))
                        .status(StudentStatus.FINISHED)
                        .startDate(today.minusWeeks(HISTORY_WEEKS - 4)).endDate(today.minusWeeks(8))
                        .leaveReason("Переехали в другой город, ездить стало неудобно")
                        .parentName("Tiiu Ilves").parentPhone("5099 1234")
                        .ignoreDuplicates(true).build());

        List<StudentDto> created = new ArrayList<>();
        for (StudentRequest request : requests) {
            created.add(studentService.create(userId, request));
        }
        return created;
    }

    // --- lessons ---

    private int createLessons(UUID userId, List<StudentDto> students) {
        List<Lesson> lessons = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            lessons.addAll(historyFor(userId, students.get(i), i));
        }
        lessons.addAll(todayFor(userId, students));
        lessonRepository.saveAll(lessons);
        return lessons.size();
    }

    /** One weekly slot per student, from their start date until they stopped (or +3 weeks). */
    private List<Lesson> historyFor(UUID userId, StudentDto student, int index) {
        List<Lesson> out = new ArrayList<>();
        DayOfWeek day = DayOfWeek.of(((index * 2) % 5) + 1);
        int hour = 15 + (index % 4);
        int minutes = switch (index % 3) {
            case 0 -> 60;
            case 1 -> 90;
            default -> 120;
        };

        // history starts when the student did, but never earlier than the seeded window
        OffsetDateTime windowStart = OffsetDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS).minusWeeks(HISTORY_WEEKS);
        OffsetDateTime studentStart = student.startDate() == null ? windowStart
                : student.startDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime first = (studentStart.isAfter(windowStart) ? studentStart : windowStart)
                .with(TemporalAdjusters.nextOrSame(day))
                .withHour(hour);
        OffsetDateTime last = student.status() == StudentStatus.FINISHED && student.endDate() != null
                ? student.endDate().atStartOfDay().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC).plusWeeks(FUTURE_WEEKS);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // the first student's rate went up in the middle of last month, so that month's report
        // contains two different prices for them and raises its multi-price warning
        OffsetDateTime raise = now.truncatedTo(ChronoUnit.DAYS)
                .withDayOfMonth(15).minusMonths(1);

        int occurrence = 0;
        for (OffsetDateTime start = first; start.isBefore(last); start = start.plusWeeks(1)) {
            occurrence++;
            BigDecimal rate = student.lessonPrice();
            if (index == 0 && start.isBefore(raise)) {
                rate = new BigDecimal("20.00");
            }
            OffsetDateTime end = start.plusMinutes(minutes);

            Lesson lesson = Lesson.create(userId, student.id());
            lesson.setStartTime(start);
            lesson.setEndTime(end);
            lesson.setPrice(LessonPricing.priceFor(rate, start, end));
            lesson.setStatus(statusFor(start, now, occurrence));
            lesson.setNotes(occurrence % 5 == 0 ? "Повторили пройденное, дал домашнее задание" : null);
            // no Google account is connected in a dev database
            lesson.setCalendarSyncStatus(CalendarSyncStatus.DISABLED);
            out.add(lesson);
        }
        return out;
    }

    /** Most past lessons happened; roughly one in six fell through, alternating cancel / no-show. */
    private static LessonStatus statusFor(OffsetDateTime start, OffsetDateTime now, int occurrence) {
        if (!start.isBefore(now)) {
            return LessonStatus.PLANNED;
        }
        if (occurrence % 6 == 0) {
            return occurrence % 12 == 0 ? LessonStatus.NO_SHOW : LessonStatus.CANCELLED;
        }
        return LessonStatus.COMPLETED;
    }

    /** Three lessons later today so the dashboard always has something to mark off. */
    private List<Lesson> todayFor(UUID userId, List<StudentDto> students) {
        List<StudentDto> active = students.stream()
                .filter(s -> s.status() == StudentStatus.ACTIVE)
                .limit(3)
                .toList();
        int[] hours = {15, 17, 19};
        int[] lengths = {60, 90, 120};

        List<Lesson> out = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            StudentDto student = active.get(i);
            OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.DAYS).withHour(hours[i]);
            OffsetDateTime end = start.plusMinutes(lengths[i]);

            Lesson lesson = Lesson.create(userId, student.id());
            lesson.setStartTime(start);
            lesson.setEndTime(end);
            lesson.setPrice(LessonPricing.priceFor(student.lessonPrice(), start, end));
            lesson.setStatus(LessonStatus.PLANNED);
            lesson.setCalendarSyncStatus(CalendarSyncStatus.DISABLED);
            out.add(lesson);
        }
        return out;
    }
}
