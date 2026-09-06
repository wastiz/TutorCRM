package com.tutormgmt.student.importer;

import com.tutormgmt.student.LessonFormat;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic value-level parsing shared by every {@link StudentImportParser}
 * implementation (CLAUDE.md sections 18–25). Pure functions only — no I/O, no state.
 */
public final class ImportValues {

    /**
     * Deliberately stops the TLD at the first non-letter so a glued
     * {@code maksim.ivanov@example.com5350} still yields {@code maksim.ivanov@example.com}.
     */
    public static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");

    private static final Pattern TIME_RANGE = Pattern.compile(
            "^(\\d{1,2})(?::(\\d{2}))?\\s*[-–—]\\s*(\\d{1,2})(?::(\\d{2}))?$");

    private static final Pattern SINGLE_TIME = Pattern.compile("^(\\d{1,2})(?::(\\d{2}))$");

    private static final Map<String, DayOfWeek> DAY_ALIASES = buildDayAliases();

    private ImportValues() {
    }

    // --- text normalization ---

    /** Strip BOM, fold line breaks away — the message is a single logical line. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("﻿", "");
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        s = s.replace('\n', ' ');
        return s.strip();
    }

    public static String collapseSpaces(String s) {
        return s.replaceAll("[ \\u00A0]{2,}", " ").trim();
    }

    /** Trim, collapse spaces and ensure a space after commas (CLAUDE.md section 58). */
    public static String normalizeProse(String s) {
        if (s == null) {
            return null;
        }
        String out = collapseSpaces(s.replaceAll(",(?=\\S)", ", "));
        return out.isEmpty() ? null : out;
    }

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = collapseSpaces(s.trim());
        return t.isEmpty() ? null : t;
    }

    // --- name ---

    /** {@code [firstName, lastName]}; the last name keeps every remaining part (section 18). */
    public static String[] splitName(String full, List<String> warnings) {
        if (full == null || full.isBlank()) {
            warnings.add("Full name is empty");
            return new String[] {null, null};
        }
        String cleaned = collapseSpaces(splitCamelCase(full.trim()));
        String[] parts = cleaned.split("\\s+", 2);
        if (parts.length == 1) {
            warnings.add("Could not determine a last name from \"" + full + "\"");
            return new String[] {parts[0], null};
        }
        return new String[] {parts[0], parts[1]};
    }

    /** {@code "MaksimIvanov"} → {@code "Maksim Ivanov"}; leaves normal text untouched. */
    public static String splitCamelCase(String s) {
        return s.replaceAll("(?<=\\p{Ll})(?=\\p{Lu})", " ");
    }

    /** True when the token reads like a person's name rather than data. */
    public static boolean looksLikeName(String token) {
        if (token == null) {
            return false;
        }
        String t = collapseSpaces(splitCamelCase(token.trim()));
        if (t.isEmpty() || t.length() > 80 || t.matches(".*\\d.*")) {
            return false;
        }
        String[] parts = t.split("\\s+");
        if (parts.length < 2 || parts.length > 4) {
            return false;
        }
        for (String p : parts) {
            if (!p.matches("\\p{Lu}\\p{L}+(?:-\\p{Lu}?\\p{L}+)?")) {
                return false;
            }
        }
        return true;
    }

    // --- numbers ---

    public static Integer parseIntOrWarn(String value, String fieldName, List<String> warnings) {
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

    public static List<DayOfWeek> parseDays(String raw, List<String> warnings) {
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

    /** Days found anywhere inside free text; unknown words are ignored rather than reported. */
    public static List<DayOfWeek> findDays(String raw) {
        Set<DayOfWeek> result = new LinkedHashSet<>();
        if (raw == null) {
            return new ArrayList<>();
        }
        for (String token : raw.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            DayOfWeek day = DAY_ALIASES.get(token);
            if (day != null) {
                result.add(day);
            }
        }
        return new ArrayList<>(result);
    }

    public static boolean isDayToken(String raw) {
        return !parseDaysQuietly(raw).isEmpty();
    }

    private static List<DayOfWeek> parseDaysQuietly(String raw) {
        List<DayOfWeek> days = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return days;
        }
        for (String token : raw.split("[,;/]| и ")) {
            String key = token.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                continue;
            }
            DayOfWeek day = DAY_ALIASES.get(key);
            if (day == null) {
                return List.of();
            }
            days.add(day);
        }
        return days;
    }

    private static Map<String, DayOfWeek> buildDayAliases() {
        Map<String, DayOfWeek> m = new HashMap<>();
        m.put("понедельник", DayOfWeek.MONDAY);
        m.put("пн", DayOfWeek.MONDAY);
        m.put("вторник", DayOfWeek.TUESDAY);
        m.put("вт", DayOfWeek.TUESDAY);
        m.put("среда", DayOfWeek.WEDNESDAY);
        m.put("среду", DayOfWeek.WEDNESDAY);
        m.put("ср", DayOfWeek.WEDNESDAY);
        m.put("четверг", DayOfWeek.THURSDAY);
        m.put("чт", DayOfWeek.THURSDAY);
        m.put("пятница", DayOfWeek.FRIDAY);
        m.put("пятницу", DayOfWeek.FRIDAY);
        m.put("пт", DayOfWeek.FRIDAY);
        m.put("суббота", DayOfWeek.SATURDAY);
        m.put("субботу", DayOfWeek.SATURDAY);
        m.put("сб", DayOfWeek.SATURDAY);
        m.put("воскресенье", DayOfWeek.SUNDAY);
        m.put("вс", DayOfWeek.SUNDAY);
        for (DayOfWeek d : DayOfWeek.values()) {
            m.put(d.name().toLowerCase(Locale.ROOT), d);
            m.put(d.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ROOT), d);
        }
        return Map.copyOf(m);
    }

    // --- time ---

    public record TimeRange(String from, String to, String unparsedRaw) {
        public static TimeRange empty() {
            return new TimeRange(null, null, null);
        }

        public boolean isEmpty() {
            return from == null && to == null && unparsedRaw == null;
        }
    }

    public static TimeRange parseTime(String raw, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return TimeRange.empty();
        }
        TimeRange parsed = parseTimeQuietly(raw);
        if (parsed.unparsedRaw() != null) {
            warnings.add("Unrecognized time format: \"" + raw + "\" (kept as-is)");
        }
        return parsed;
    }

    /** Same as {@link #parseTime} but silent — used when probing an unlabelled token. */
    public static TimeRange parseTimeQuietly(String raw) {
        if (raw == null || raw.isBlank()) {
            return TimeRange.empty();
        }
        String cleaned = raw.trim().replaceAll("\\s*([-–—])\\s*", "-");
        Matcher m = TIME_RANGE.matcher(cleaned);
        if (!m.matches()) {
            return new TimeRange(null, null, raw);
        }
        String from = toHm(m.group(1), m.group(2));
        String to = toHm(m.group(3), m.group(4));
        if (from == null || to == null) {
            return new TimeRange(null, null, raw);
        }
        return new TimeRange(from, to, null);
    }

    /** True when the token is a time range ({@code 17-20}) or a single clock time ({@code 17:00}). */
    public static boolean isTimeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String cleaned = raw.trim().replaceAll("\\s*([-–—])\\s*", "-");
        return TIME_RANGE.matcher(cleaned).matches() || SINGLE_TIME.matcher(cleaned).matches();
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

    public static LessonFormat parseFormat(String raw, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        LessonFormat format = findFormat(raw);
        if (format == null) {
            warnings.add("Unrecognized lesson format: \"" + raw + "\"");
        }
        return format;
    }

    /** Case-insensitive format detection (CLAUDE.md section 23); {@code null} when absent. */
    public static LessonFormat findFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        boolean online = v.contains("онлайн") || v.contains("online");
        boolean offline = v.contains("офлайн") || v.contains("оффлайн") || v.contains("offline")
                || v.contains("вживую") || v.contains("контактн");
        if (v.contains("оба") || v.equals("both") || (online && offline)) {
            return LessonFormat.BOTH;
        }
        if (online) {
            return LessonFormat.ONLINE;
        }
        if (offline) {
            return LessonFormat.OFFLINE;
        }
        return null;
    }
}
