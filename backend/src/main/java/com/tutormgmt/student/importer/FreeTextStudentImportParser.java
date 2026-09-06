package com.tutormgmt.student.importer;

import com.tutormgmt.student.LessonFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parser for messages that are not TAB-delimited: free text, one value per line, or values
 * glued together without any separator at all.
 *
 * <p>Nothing is read by position here. Every field is recognized by its own shape — e-mail,
 * isikukood and phone via {@link ContactScanner}, then day names, a time range, the lesson
 * format, age/grade/lessons-per-week keywords and finally capitalized word pairs as names.
 * Whatever text is left over becomes the goal, so no information is silently lost
 * (CLAUDE.md section 18/24).
 */
@Component
public class FreeTextStudentImportParser implements StudentImportParser {

    private static final Pattern PARENT_MARKER = Pattern.compile(
            "родител|мама|мамы|мать|матери|папа|папы|отец|отца|parent|mother|father|ema|isa",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TIME_RANGE = Pattern.compile(
            "\\b(\\d{1,2}(?::\\d{2})?)\\s*[-–—]\\s*(\\d{1,2}(?::\\d{2})?)\\b");

    private static final Pattern AGE = Pattern.compile(
            "\\b(\\d{1,2})\\s*(?:лет|год[ауа]?|years?|y\\.?o\\.?|aastat)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern GRADE = Pattern.compile(
            "\\b(\\d{1,2})\\s*(?:-?\\s*(?:й|ый|ой))?\\s*(?:класс\\w*|kl\\.?|klass|grade)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern LESSONS_PER_WEEK = Pattern.compile(
            "\\b(\\d)\\s*(?:раз\\w*|x|х)\\s*(?:в\\s*неделю|/\\s*неделю|per\\s*week)?\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern TELEGRAM = Pattern.compile(
            "(?:(?:https?://)?t\\.me/|@)([A-Za-z][A-Za-z0-9_]{4,31})");

    private static final String NAME_WORD = "\\p{Lu}\\p{L}+(?:-\\p{Lu}?\\p{L}+)?";

    /** Two to four capitalized words, or a single glued {@code AnnaIvanova}. */
    private static final Pattern NAME = Pattern.compile(
            "(?:" + NAME_WORD + "(?:\\s+" + NAME_WORD + "){1,3})|(?:\\p{Lu}\\p{Ll}+\\p{Lu}\\p{Ll}+)");

    /**
     * Capitalized words that open a message or label a value — never part of a name.
     * They are hidden from the name scan only, so they still reach the goal text.
     */
    private static final Pattern NON_NAME_WORD = Pattern.compile(
            "\\b(?:здравствуйте|привет|добрый|доброе|день|вечер|утро|ученик\\w*|ученица|студент\\w*|"
                    + "мама|мамы|мать|матери|папа|папы|отец|отца|родител\\w*|телефон|тел|почта|"
                    + "занятия|занятие|урок\\w*|класс\\w*|школа|предмет|цель|"
                    + "hello|hi|student|parent|mother|father|phone|mail|email|lessons?|school|subject)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern FORMAT_WORD = Pattern.compile(
            "оба варианта подходят|оба варианта|онлайн|офлайн|оффлайн|online|offline|both",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DAY_WORD = Pattern.compile(
            "\\p{L}+", Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public StudentImportPreviewDto parse(String rawText) {
        List<String> warnings = new ArrayList<>();
        String text = ImportValues.normalize(rawText).replace('\t', ' ');
        if (text.isBlank()) {
            warnings.add("Nothing to parse — the message is empty");
            return new StudentImportPreviewDto(ImportedStudentDto.builder().build(), warnings, 0,
                    rawText == null ? "" : rawText);
        }

        Residual residual = new Residual(text);
        int parentFrom = parentBoundary(text);

        List<ContactScanner.Item> items = new ArrayList<>();
        for (ContactScanner.Match m : ContactScanner.find(text)) {
            items.add(new ContactScanner.Item(m.kind(), m.value(), m.start() >= parentFrom));
            residual.blank(m.start(), m.end());
        }

        // scanned on the residual so the "@" of an already-recognized e-mail cannot match
        String telegram = firstGroup(TELEGRAM, residual.raw(), residual);
        Integer age = firstInt(AGE, text, residual);
        Integer grade = firstInt(GRADE, text, residual);
        Integer lessonsPerWeek = firstInt(LESSONS_PER_WEEK, text, residual);

        ContactScanner.Contacts contacts = ContactScanner.assign(items, age, warnings);
        if (age == null && contacts.isikukood() != null) {
            age = Isikukood.ageOn(contacts.isikukood(), LocalDate.now()).orElse(null);
            if (age != null) {
                warnings.add("Age was missing — derived " + age + " from the isikukood");
            }
        }

        ImportValues.TimeRange time = ImportValues.TimeRange.empty();
        Matcher tm = TIME_RANGE.matcher(text);
        if (tm.find()) {
            time = ImportValues.parseTimeQuietly(tm.group());
            residual.blank(tm.start(), tm.end());
        }

        List<DayOfWeek> days = takeDays(text, residual);

        LessonFormat format = null;
        Matcher fm = FORMAT_WORD.matcher(text);
        StringBuilder formatText = new StringBuilder();
        while (fm.find()) {
            formatText.append(fm.group()).append(' ');
            residual.blank(fm.start(), fm.end());
        }
        if (!formatText.isEmpty()) {
            format = ImportValues.findFormat(formatText.toString());
        }

        String[] names = takeNames(residual, parentFrom);
        String[] studentName = ImportValues.splitName(names[0], warnings);

        String goal = ImportValues.normalizeProse(residual.text());

        ImportedStudentDto student = ImportedStudentDto.builder()
                .firstName(studentName[0])
                .lastName(studentName[1])
                .email(contacts.email())
                .phone(contacts.phone())
                .isikukood(contacts.isikukood())
                .age(age)
                .grade(grade)
                .goal(goal)
                .lessonFormat(format)
                .parentName(names[1])
                .parentPhone(contacts.parentPhone())
                .parentEmail(contacts.parentEmail())
                .parentIsikukood(contacts.parentIsikukood())
                .parentSecondaryPhone(contacts.parentSecondaryPhone())
                .telegram(telegram)
                .preferredDays(days)
                .preferredTimeFrom(time.from())
                .preferredTimeTo(time.to())
                .preferredTimeRaw(time.unparsedRaw())
                .lessonsPerWeek(lessonsPerWeek)
                .schedules(schedules(days, time, lessonsPerWeek))
                .build();

        if (student.email() == null) {
            warnings.add("No e-mail found in the message — fill it in before saving");
        }
        return new StudentImportPreviewDto(student, warnings, 1, rawText == null ? "" : rawText);
    }

    // --- internals ---

    /** Character offset from which the message talks about the parent. */
    private static int parentBoundary(String text) {
        Matcher m = PARENT_MARKER.matcher(text);
        return m.find() ? m.start() : Integer.MAX_VALUE;
    }

    private static List<DayOfWeek> takeDays(String text, Residual residual) {
        List<DayOfWeek> days = new ArrayList<>();
        Matcher m = DAY_WORD.matcher(text);
        while (m.find()) {
            List<DayOfWeek> found = ImportValues.findDays(m.group());
            if (!found.isEmpty()) {
                found.stream().filter(d -> !days.contains(d)).forEach(days::add);
                residual.blank(m.start(), m.end());
            }
        }
        return days;
    }

    /** {@code [studentName, parentName]} from the capitalized word pairs that are left. */
    private static String[] takeNames(Residual residual, int parentFrom) {
        String student = null;
        String parent = null;
        Matcher m = NAME.matcher(hideNonNameWords(residual.raw()));
        while (m.find()) {
            String candidate = ImportValues.collapseSpaces(m.group());
            if (!ImportValues.findDays(candidate).isEmpty()) {
                continue;
            }
            boolean parentSide = m.start() >= parentFrom;
            if (!parentSide && student == null) {
                student = candidate;
                residual.blank(m.start(), m.end());
            } else if (parent == null) {
                parent = candidate;
                residual.blank(m.start(), m.end());
            }
        }
        if (student == null && parent != null) {
            student = parent;
            parent = null;
        }
        return new String[] {student, parent};
    }

    /** Blank out label words so they cannot be mistaken for a first name. */
    private static String hideNonNameWords(String text) {
        StringBuilder sb = new StringBuilder(text);
        Matcher m = NON_NAME_WORD.matcher(text);
        while (m.find()) {
            for (int i = m.start(); i < m.end(); i++) {
                sb.setCharAt(i, ' ');
            }
        }
        return sb.toString();
    }

    private static String firstGroup(Pattern pattern, String text, Residual residual) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) {
            return null;
        }
        residual.blank(m.start(), m.end());
        return m.group(1);
    }

    private static Integer firstInt(Pattern pattern, String text, Residual residual) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) {
            return null;
        }
        residual.blank(m.start(), m.end());
        try {
            return Integer.valueOf(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<ImportedStudentDto.ScheduleEntryDto> schedules(
            List<DayOfWeek> days, ImportValues.TimeRange time, Integer lessonsPerWeek) {
        List<ImportedStudentDto.ScheduleEntryDto> schedules = new ArrayList<>();
        for (DayOfWeek day : days) {
            schedules.add(new ImportedStudentDto.ScheduleEntryDto(
                    day, time.from(), time.to(), lessonsPerWeek));
        }
        return schedules;
    }

    /** The message with every already-recognized span blanked out, offsets preserved. */
    private static final class Residual {

        private final char[] chars;

        Residual(String text) {
            this.chars = text.toCharArray();
        }

        void blank(int start, int end) {
            for (int i = Math.max(0, start); i < Math.min(end, chars.length); i++) {
                chars[i] = ' ';
            }
        }

        /** Offsets here still line up with the original message. */
        String raw() {
            return new String(chars);
        }

        String text() {
            return new String(chars)
                    .replaceAll("[,;|]+", " ")
                    .replaceAll("\\s{2,}", " ")
                    .trim();
        }
    }
}
