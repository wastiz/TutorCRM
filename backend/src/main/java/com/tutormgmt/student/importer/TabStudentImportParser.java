package com.tutormgmt.student.importer;

import com.tutormgmt.student.LessonFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 *  6 Subject                15 Parent phone
 *  7 Goal / description     16 Parent secondary phone
 *  8 Preferred days
 * </pre>
 * A 16-field message is read with the section-14 layout (parent phone at 14, secondary at 15).
 * Any other count still parses positionally and adds a warning — never throws.
 */
@Component
public class TabStudentImportParser implements StudentImportParser {

    private static final int EXPECTED_MIN = 16;
    private static final int EXPECTED_MAX = 17;

    private static final Map<String, DayOfWeek> DAY_ALIASES = buildDayAliases();

    private static final Pattern TIME_RANGE = Pattern.compile(
            "^(\\d{1,2})(?::(\\d{2}))?\\s*[-–—]\\s*(\\d{1,2})(?::(\\d{2}))?$");

    @Override
    public StudentImportPreviewDto parse(String rawText) {
        List<String> warnings = new ArrayList<>();

        String normalized = normalize(rawText);
        String[] fields = normalized.split("\t", -1);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = collapseSpaces(fields[i].trim());
        }
        int count = fields.length;
        if (count < EXPECTED_MIN || count > EXPECTED_MAX) {
            warnings.add("Unexpected number of fields: expected " + EXPECTED_MIN + "–" + EXPECTED_MAX
                    + ", got " + count);
        }
        boolean layout17 = count >= EXPECTED_MAX;

        String[] name = splitName(get(fields, 0), warnings);

        Integer age = parseIntOrWarn(get(fields, 3), "age", warnings);
        Integer grade = parseIntOrWarn(get(fields, 4), "grade", warnings);
        Integer lessonsPerWeek = parseIntOrWarn(get(fields, 10), "lessons per week", warnings);

        List<DayOfWeek> days = parseDays(get(fields, 8), warnings);
        TimeRange time = parseTime(get(fields, 9), warnings);
        LessonFormat format = parseFormat(get(fields, 11), warnings);

        String parentName = get(fields, 13);
        String parentPhone = layout17 ? get(fields, 15) : get(fields, 14);
        String parentSecondaryPhone = layout17 ? get(fields, 16) : get(fields, 15);

        List<ImportedStudentDto.ScheduleEntryDto> schedules = new ArrayList<>();
        for (DayOfWeek day : days) {
            schedules.add(new ImportedStudentDto.ScheduleEntryDto(day, time.from(), time.to(), lessonsPerWeek));
        }

        ImportedStudentDto student = new ImportedStudentDto(
                name[0],
                name[1],
                get(fields, 1),
                get(fields, 2),
                null,
                age,
                grade,
                get(fields, 5),
                get(fields, 6),
                normalizeProse(get(fields, 7)),
                null,
                null,
                format,
                null,
                parentName,
                parentPhone,
                null,
                parentSecondaryPhone,
                days,
                time.from(),
                time.to(),
                time.unparsedRaw(),
                lessonsPerWeek,
                schedules);

        return new StudentImportPreviewDto(student, warnings, count, rawText == null ? "" : rawText);
    }

    // --- normalization ---

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\uFEFF", "");           // strip BOM anywhere
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        s = s.replace('\n', ' ');                       // the message is a single logical line
        return s.strip();
    }

    private static String collapseSpaces(String s) {
        return s.replaceAll("[ \\u00A0]{2,}", " ").trim();
    }

    /** Trim, collapse spaces, and ensure a space after commas (CLAUDE.md section 58 expected output). */
    private static String normalizeProse(String s) {
        if (s == null) {
            return null;
        }
        String out = collapseSpaces(s.replaceAll(",(?=\\S)", ", "));
        return out.isEmpty() ? null : out;
    }

    // --- name ---

    private static String[] splitName(String full, List<String> warnings) {
        if (full == null || full.isBlank()) {
            warnings.add("Full name is empty");
            return new String[] {null, null};
        }
        String[] parts = full.trim().split("\\s+", 2);
        if (parts.length == 1) {
            warnings.add("Could not determine a last name from \"" + full + "\"");
            return new String[] {parts[0], null};
        }
        return new String[] {parts[0], parts[1]};
    }

    // --- numbers ---

    private static Integer parseIntOrWarn(String value, String fieldName, List<String> warnings) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            warnings.add("Could not read " + fieldName + " from \"" + value + "\"");
            return null;
        }
    }

    // --- days ---

    private static List<DayOfWeek> parseDays(String raw, List<String> warnings) {
        Set<DayOfWeek> result = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        for (String token : raw.split("[,;/]| и ")) {
            String key = token.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                continue;
            }
            DayOfWeek day = DAY_ALIASES.get(key);
            if (day != null) {
                result.add(day);
            } else {
                warnings.add("Unrecognized day: \"" + token.trim() + "\"");
            }
        }
        return new ArrayList<>(result);
    }

    private static Map<String, DayOfWeek> buildDayAliases() {
        Map<String, DayOfWeek> m = new java.util.HashMap<>();
        m.put("понедельник", DayOfWeek.MONDAY);
        m.put("вторник", DayOfWeek.TUESDAY);
        m.put("среда", DayOfWeek.WEDNESDAY);
        m.put("среду", DayOfWeek.WEDNESDAY);
        m.put("четверг", DayOfWeek.THURSDAY);
        m.put("пятница", DayOfWeek.FRIDAY);
        m.put("пятницу", DayOfWeek.FRIDAY);
        m.put("суббота", DayOfWeek.SATURDAY);
        m.put("субботу", DayOfWeek.SATURDAY);
        m.put("воскресенье", DayOfWeek.SUNDAY);
        for (DayOfWeek d : DayOfWeek.values()) {
            m.put(d.name().toLowerCase(Locale.ROOT), d);
            m.put(d.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ROOT), d);
        }
        return Map.copyOf(m);
    }

    // --- time ---

    private record TimeRange(String from, String to, String unparsedRaw) {
        static TimeRange empty() {
            return new TimeRange(null, null, null);
        }
    }

    private static TimeRange parseTime(String raw, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return TimeRange.empty();
        }
        String cleaned = raw.trim().replaceAll("\\s*([-–—])\\s*", "-");
        Matcher m = TIME_RANGE.matcher(cleaned);
        if (!m.matches()) {
            warnings.add("Unrecognized time format: \"" + raw + "\" (kept as-is)");
            return new TimeRange(null, null, raw);
        }
        String from = toHm(m.group(1), m.group(2));
        String to = toHm(m.group(3), m.group(4));
        if (from == null || to == null) {
            warnings.add("Time out of range: \"" + raw + "\" (kept as-is)");
            return new TimeRange(null, null, raw);
        }
        return new TimeRange(from, to, null);
    }

    private static String toHm(String hh, String mm) {
        int h = Integer.parseInt(hh);
        int m = mm == null ? 0 : Integer.parseInt(mm);
        if (h < 0 || h > 23 || m < 0 || m > 59) {
            return null;
        }
        return String.format("%02d:%02d", h, m);
    }

    // --- lesson format ---

    private static LessonFormat parseFormat(String raw, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.contains("оба") || v.equals("both") || (v.contains("онлайн") && v.contains("офлайн"))) {
            return LessonFormat.BOTH;
        }
        if (v.contains("онлайн") || v.contains("online")) {
            return LessonFormat.ONLINE;
        }
        if (v.contains("офлайн") || v.contains("оффлайн") || v.contains("offline")) {
            return LessonFormat.OFFLINE;
        }
        warnings.add("Unrecognized lesson format: \"" + raw + "\"");
        return null;
    }

    // --- util ---

    private static String get(String[] fields, int index) {
        if (index < 0 || index >= fields.length) {
            return null;
        }
        String v = fields[index];
        return (v == null || v.isEmpty()) ? null : v;
    }
}
