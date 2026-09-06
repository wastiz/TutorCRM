package com.tutormgmt.email;

import com.tutormgmt.lesson.Lesson;
import com.tutormgmt.lesson.LessonPricing;
import com.tutormgmt.student.Student;
import com.tutormgmt.user.User;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The variables a template may use, and how they are filled from the domain objects.
 *
 * <p>Kept in one place so the Settings screen can list exactly what is supported
 * ({@link #catalogue()}) and the renderer can never disagree with that list.
 */
public final class EmailPlaceholders {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private EmailPlaceholders() {
    }

    /** Name → what it means, for the template editor's help panel. */
    public static List<EmailDto.PlaceholderDto> catalogue() {
        return List.of(
                new EmailDto.PlaceholderDto("student.firstName", "Student's first name"),
                new EmailDto.PlaceholderDto("student.lastName", "Student's last name"),
                new EmailDto.PlaceholderDto("student.fullName", "Student's full name"),
                new EmailDto.PlaceholderDto("student.email", "Student's e-mail"),
                new EmailDto.PlaceholderDto("student.subject", "Subject taught"),
                new EmailDto.PlaceholderDto("student.rate", "Rate per 60 min, €"),
                new EmailDto.PlaceholderDto("parent.name", "Parent's name"),
                new EmailDto.PlaceholderDto("parent.email", "Parent's e-mail"),
                new EmailDto.PlaceholderDto("lesson.date", "Lesson date (dd.MM.yyyy)"),
                new EmailDto.PlaceholderDto("lesson.time", "Lesson start time (HH:mm)"),
                new EmailDto.PlaceholderDto("lesson.duration", "Lesson length, e.g. 1.5 h"),
                new EmailDto.PlaceholderDto("lesson.price", "Lesson price, €"),
                new EmailDto.PlaceholderDto("tutor.name", "Your name"),
                new EmailDto.PlaceholderDto("tutor.email", "Your e-mail"),
                new EmailDto.PlaceholderDto("today", "Today's date (dd.MM.yyyy)"));
    }

    public static Map<String, String> of(User tutor, Student student, Lesson lesson) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("today", LocalDate.now().format(DATE));
        if (tutor != null) {
            values.put("tutor.name", fullName(tutor.getFirstName(), tutor.getLastName()));
            values.put("tutor.email", nullToEmpty(tutor.getEmail()));
        }
        if (student != null) {
            values.put("student.firstName", nullToEmpty(student.getFirstName()));
            values.put("student.lastName", nullToEmpty(student.getLastName()));
            values.put("student.fullName", fullName(student.getFirstName(), student.getLastName()));
            values.put("student.email", nullToEmpty(student.getEmail()));
            values.put("student.subject", nullToEmpty(student.getSubject()));
            values.put("student.rate", student.getLessonPrice() == null
                    ? "" : student.getLessonPrice().toPlainString());
            values.put("parent.name", nullToEmpty(student.getParentName()));
            values.put("parent.email", nullToEmpty(student.getParentEmail()));
        }
        if (lesson != null) {
            values.put("lesson.date", lesson.getStartTime().format(DATE));
            values.put("lesson.time", lesson.getStartTime().format(TIME));
            values.put("lesson.duration", durationLabel(lesson));
            values.put("lesson.price", lesson.getPrice() == null ? "" : lesson.getPrice().toPlainString());
        }
        return values;
    }

    private static String durationLabel(Lesson lesson) {
        long minutes = LessonPricing.minutesBetween(lesson.getStartTime(), lesson.getEndTime());
        if (minutes % 60 == 0) {
            return (minutes / 60) + " h";
        }
        return minutes == 90 ? "1.5 h" : minutes + " min";
    }

    private static String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
