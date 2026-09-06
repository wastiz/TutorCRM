package com.tutormgmt.student.importer;

import com.tutormgmt.student.LessonFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Deterministic TAB-delimited parser (CLAUDE.md sections 13, 14, 18–25).
 *
 * <p>Canonical 17-field layout (matches the real examples in sections 57/58):
 * <pre>
 *  0 Full name              9  Preferred time
 *  1 Student email          10 Lessons per week
 *  2 Student phone          11 Lesson format
 *  3 Age                    12 (reserved)
 *  4 Grade                  13 Parent name
 *  5 School                 14 (reserved)
 *  6 Subject                15 Student isikukood
 *  7 Goal / description     16 Parent isikukood
 *  8 Preferred days
 * </pre>
 *
 * <p>Positions drive only the fields that cannot be told apart by their content
 * (school, subject, goal, grade…). E-mails, phone numbers and personal codes are recognized
 * by {@link ContactScanner} wherever they appear, so a shifted or partly glued message still
 * lands in the right fields. Any field count still parses and only adds a warning — never throws.
 */
@Component
public class TabStudentImportParser implements StudentImportParser {

    private static final int EXPECTED_MIN = 16;
    private static final int EXPECTED_MAX = 17;
    private static final int PARENT_NAME_COLUMN = 13;

    @Override
    public StudentImportPreviewDto parse(String rawText) {
        List<String> warnings = new ArrayList<>();

        String[] fields = ImportValues.normalize(rawText).split("\t", -1);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = ImportValues.collapseSpaces(fields[i].trim());
        }
        int count = fields.length;
        if (count < EXPECTED_MIN || count > EXPECTED_MAX) {
            warnings.add("Unexpected number of fields: expected " + EXPECTED_MIN + "–" + EXPECTED_MAX
                    + ", got " + count);
        }

        String[] name = ImportValues.splitName(get(fields, 0), warnings);

        Integer age = ImportValues.parseIntOrWarn(get(fields, 3), "age", warnings);
        Integer grade = ImportValues.parseIntOrWarn(get(fields, 4), "grade", warnings);
        Integer lessonsPerWeek = ImportValues.parseIntOrWarn(get(fields, 10), "lessons per week", warnings);

        List<DayOfWeek> days = ImportValues.parseDays(get(fields, 8), warnings);
        ImportValues.TimeRange time = ImportValues.parseTime(get(fields, 9), warnings);
        LessonFormat format = ImportValues.parseFormat(get(fields, 11), warnings);

        int parentColumn = parentNameColumn(fields);
        String parentName = get(fields, parentColumn);
        ContactScanner.Contacts contacts =
                ContactScanner.assign(scanColumns(fields, parentColumn), age, warnings);

        if (age == null && contacts.isikukood() != null) {
            age = Isikukood.ageOn(contacts.isikukood(), LocalDate.now()).orElse(null);
            if (age != null) {
                warnings.add("Age was missing — derived " + age + " from the isikukood");
            }
        }

        ImportedStudentDto student = ImportedStudentDto.builder()
                .firstName(name[0])
                .lastName(name[1])
                .email(contacts.email())
                .phone(contacts.phone())
                .isikukood(contacts.isikukood())
                .age(age)
                .grade(grade)
                .school(get(fields, 5))
                .subject(get(fields, 6))
                .goal(ImportValues.normalizeProse(get(fields, 7)))
                .lessonFormat(format)
                .parentName(parentName)
                .parentPhone(contacts.parentPhone())
                .parentEmail(contacts.parentEmail())
                .parentIsikukood(contacts.parentIsikukood())
                .parentSecondaryPhone(contacts.parentSecondaryPhone())
                .preferredDays(days)
                .preferredTimeFrom(time.from())
                .preferredTimeTo(time.to())
                .preferredTimeRaw(time.unparsedRaw())
                .lessonsPerWeek(lessonsPerWeek)
                .schedules(schedules(days, time, lessonsPerWeek))
                .build();

        return new StudentImportPreviewDto(student, warnings, count, rawText == null ? "" : rawText);
    }

    // --- internals ---

    /** Everything from the parent-name column onwards describes the parent, not the student. */
    private static List<ContactScanner.Item> scanColumns(String[] fields, int parentColumn) {
        List<ContactScanner.Item> items = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            boolean parentSide = i >= parentColumn;
            for (ContactScanner.Match m : ContactScanner.find(fields[i])) {
                items.add(new ContactScanner.Item(m.kind(), m.value(), parentSide));
            }
        }
        return items;
    }

    /** The last name-shaped column (the parent), falling back to the canonical position. */
    private static int parentNameColumn(String[] fields) {
        for (int i = fields.length - 1; i > 0; i--) {
            String v = fields[i];
            if (ImportValues.looksLikeName(v) && ImportValues.findDays(v).isEmpty()) {
                return i;
            }
        }
        return PARENT_NAME_COLUMN;
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

    private static String get(String[] fields, int index) {
        if (index < 0 || index >= fields.length) {
            return null;
        }
        String v = fields[index];
        return (v == null || v.isEmpty()) ? null : v;
    }
}
