package com.tutormgmt.student.importer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Content-based recognition of the identity fields in a raw message: e-mail addresses,
 * Estonian personal codes and phone numbers (CLAUDE.md sections 13/19, extended).
 *
 * <p>Recognition does not rely on the position of a value, which is what makes the import
 * tolerant of a changed field order, of missing separators and of glued values such as
 * {@code ljulap@gmail.com5350 6894}. A value is classified by what it is:
 * <ul>
 *   <li>11 digits with a valid isikukood century/date/checksum → {@code isikukood};</li>
 *   <li>7–12 digits (optionally {@code +372}-prefixed, spaces and dashes allowed) → phone;</li>
 *   <li>anything matching the e-mail pattern → e-mail.</li>
 * </ul>
 */
public final class ContactScanner {

    private static final int PHONE_MIN_DIGITS = 7;
    private static final int PHONE_MAX_DIGITS = 12;

    private ContactScanner() {
    }

    public enum Kind { EMAIL, ISIKUKOOD, PHONE }

    /** A recognized value together with where it was found in the scanned text. */
    public record Match(Kind kind, String value, int start, int end) {}

    /** A recognized value tagged with the side of the message it belongs to. */
    public record Item(Kind kind, String value, boolean parentSide) {}

    /** Everything the scanner could assign (CLAUDE.md section 10.3 field names). */
    public record Contacts(
            String email,
            String phone,
            String isikukood,
            String parentEmail,
            String parentPhone,
            String parentIsikukood,
            String parentSecondaryPhone) {

        public static Contacts empty() {
            return new Contacts(null, null, null, null, null, null, null);
        }
    }

    // --- recognition ---

    /** All e-mails, isikukoods and phones inside {@code text}, ordered by position. */
    public static List<Match> find(String text) {
        List<Match> matches = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return matches;
        }
        boolean[] taken = new boolean[text.length()];

        Matcher em = ImportValues.EMAIL.matcher(text);
        while (em.find()) {
            matches.add(new Match(Kind.EMAIL, em.group(), em.start(), em.end()));
            markTaken(taken, em.start(), em.end());
        }

        for (Match number : numbers(text, taken)) {
            matches.addAll(classifyNumber(number));
        }
        matches.sort(Comparator.comparingInt(Match::start));
        return matches;
    }

    /** Convenience for callers that scan a single field and only care about the kind. */
    public static Optional<Match> findFirst(String text, Kind kind) {
        return find(text).stream().filter(m -> m.kind() == kind).findFirst();
    }

    // --- assignment ---

    /**
     * Distribute recognized values over the student and parent slots.
     *
     * @param items    recognized values in reading order
     * @param knownAge the student's age when the message stated it — used to decide which of two
     *                 personal codes is the student's
     */
    public static Contacts assign(List<Item> items, Integer knownAge, List<String> warnings) {
        List<Item> emails = itemsOf(items, Kind.EMAIL);
        List<Item> codes = itemsOf(items, Kind.ISIKUKOOD);
        List<Item> phones = itemsOf(items, Kind.PHONE);

        String email = firstOf(emails, false);
        String parentEmail = firstOf(emails, true);
        if (parentEmail == null && emails.size() > 1) {
            parentEmail = emails.get(1).value();
        }
        if (email == null && parentEmail == null && !emails.isEmpty()) {
            email = emails.get(0).value();
        }

        String[] assignedCodes = assignIsikukoods(codes, knownAge);

        String phone = firstOf(phones, false);
        String parentPhone = firstOf(phones, true);
        String parentSecondaryPhone = secondOf(phones, true);
        List<String> studentSidePhones = phones.stream()
                .filter(i -> !i.parentSide()).map(Item::value).toList();
        if (parentPhone == null && studentSidePhones.size() > 1) {
            parentPhone = studentSidePhones.get(1);
        }
        if (parentSecondaryPhone == null && studentSidePhones.size() > 2) {
            parentSecondaryPhone = studentSidePhones.get(2);
        }
        if (phone == null && parentPhone != null && phones.stream().noneMatch(i -> !i.parentSide())) {
            // every phone sits on the parent side — keep them there, the student simply has none
            warnings.add("No student phone found; the phone numbers were read as the parent's");
        }
        long assignedPhones = countNonNull(phone, parentPhone, parentSecondaryPhone);
        if (phones.size() > assignedPhones) {
            warnings.add("Found " + phones.size() + " phone numbers; kept the first "
                    + assignedPhones + " — check the preview");
        }
        return new Contacts(email, phone, assignedCodes[0],
                parentEmail, parentPhone, assignedCodes[1], parentSecondaryPhone);
    }

    /** {@code [studentIsikukood, parentIsikukood]} — the younger person is the student. */
    private static String[] assignIsikukoods(List<Item> codes, Integer knownAge) {
        if (codes.isEmpty()) {
            return new String[] {null, null};
        }
        if (codes.size() == 1) {
            Item only = codes.get(0);
            boolean parent = only.parentSide() && !matchesAge(only.value(), knownAge);
            return parent ? new String[] {null, only.value()} : new String[] {only.value(), null};
        }
        List<Item> sorted = new ArrayList<>(codes);
        // the student is the youngest person mentioned; an explicit age wins over that guess
        sorted.sort(Comparator.comparing(
                (Item i) -> Isikukood.birthDate(i.value()).orElse(LocalDate.MIN)).reversed());
        Item student = sorted.stream()
                .filter(i -> matchesAge(i.value(), knownAge))
                .findFirst()
                .orElse(sorted.get(0));
        Item parent = sorted.stream().filter(i -> i != student).findFirst().orElse(null);
        return new String[] {student.value(), parent == null ? null : parent.value()};
    }

    private static boolean matchesAge(String isikukood, Integer knownAge) {
        if (knownAge == null) {
            return false;
        }
        return Isikukood.ageOn(isikukood, LocalDate.now())
                .map(age -> Math.abs(age - knownAge) <= 1)
                .orElse(false);
    }

    // --- number recognition internals ---

    /** Maximal digit runs, merged across single spaces/dashes while they can still form one number. */
    private static List<Match> numbers(String text, boolean[] taken) {
        record Run(int start, int end) {}
        List<Run> runs = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            if (Character.isDigit(text.charAt(i)) && !taken[i]) {
                int start = i;
                while (i < text.length() && Character.isDigit(text.charAt(i)) && !taken[i]) {
                    i++;
                }
                runs.add(new Run(start, i));
            } else {
                i++;
            }
        }

        List<Match> merged = new ArrayList<>();
        int r = 0;
        while (r < runs.size()) {
            int start = runs.get(r).start();
            int end = runs.get(r).end();
            while (r + 1 < runs.size()
                    && isPhoneSeparator(text, end, runs.get(r + 1).start())
                    && !Isikukood.isValid(text.substring(start, end).replaceAll("\\D", ""))
                    && digitsOf(text, start, runs.get(r + 1).end()) <= PHONE_MAX_DIGITS) {
                r++;
                end = runs.get(r).end();
            }
            int prefixed = start;
            if (start > 0 && text.charAt(start - 1) == '+') {
                prefixed = start - 1;
            }
            merged.add(new Match(Kind.PHONE, text.substring(prefixed, end), prefixed, end));
            r++;
        }
        return merged;
    }

    private static int digitsOf(String text, int start, int end) {
        return (int) text.substring(start, end).chars().filter(Character::isDigit).count();
    }

    /** Only a single space or dash may sit between two groups of the same number. */
    private static boolean isPhoneSeparator(String text, int from, int to) {
        if (to - from != 1) {
            return false;
        }
        char c = text.charAt(from);
        return c == ' ' || c == '-' || c == ' ';
    }

    /** An isikukood, a phone, both (when glued together), or nothing at all. */
    private static List<Match> classifyNumber(Match candidate) {
        String raw = candidate.value();
        String digits = raw.replaceAll("\\D", "");
        boolean international = raw.startsWith("+");

        if (Isikukood.isValid(digits)) {
            return List.of(new Match(Kind.ISIKUKOOD, digits, candidate.start(), candidate.end()));
        }
        if (digits.length() > 11) {
            // e.g. a phone glued to a personal code — peel the code off either end
            String head = digits.substring(0, 11);
            String tail = digits.substring(digits.length() - 11);
            if (Isikukood.isValid(tail) && isPhoneLength(digits.length() - 11)) {
                return List.of(
                        new Match(Kind.PHONE, digits.substring(0, digits.length() - 11),
                                candidate.start(), candidate.end()),
                        new Match(Kind.ISIKUKOOD, tail, candidate.start(), candidate.end()));
            }
            if (Isikukood.isValid(head) && isPhoneLength(digits.length() - 11)) {
                return List.of(
                        new Match(Kind.ISIKUKOOD, head, candidate.start(), candidate.end()),
                        new Match(Kind.PHONE, digits.substring(11), candidate.start(), candidate.end()));
            }
        }
        if (isPhoneLength(digits.length()) || (international && digits.length() >= PHONE_MIN_DIGITS)) {
            return List.of(new Match(Kind.PHONE, raw.trim(), candidate.start(), candidate.end()));
        }
        return List.of();
    }

    private static boolean isPhoneLength(int digits) {
        return digits >= PHONE_MIN_DIGITS && digits <= PHONE_MAX_DIGITS;
    }

    // --- small utils ---

    private static void markTaken(boolean[] taken, int start, int end) {
        for (int i = start; i < end && i < taken.length; i++) {
            taken[i] = true;
        }
    }

    private static List<Item> itemsOf(List<Item> items, Kind kind) {
        return items.stream().filter(i -> i.kind() == kind).toList();
    }

    private static String firstOf(List<Item> items, boolean parentSide) {
        return items.stream().filter(i -> i.parentSide() == parentSide)
                .map(Item::value).findFirst().orElse(null);
    }

    private static String secondOf(List<Item> items, boolean parentSide) {
        return items.stream().filter(i -> i.parentSide() == parentSide)
                .map(Item::value).skip(1).findFirst().orElse(null);
    }

    private static long countNonNull(String... values) {
        return java.util.Arrays.stream(values).filter(v -> v != null).count();
    }
}
