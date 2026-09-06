package com.tutormgmt.student.importer;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * Estonian personal identification code (isikukood).
 *
 * <p>Layout {@code GYYMMDDSSSC}: gender/century digit, birth date, a 3-digit serial and a
 * checksum digit. Recognising it by content matters for the import parser — an 11-digit
 * isikukood looks superficially like a long phone number, but Estonian phone numbers are
 * 7–8 digits (or prefixed with {@code +372}).
 */
public final class Isikukood {

    private static final int LENGTH = 11;

    private Isikukood() {
    }

    /** Digits only, or {@code null} when the input holds no digits. */
    public static String digits(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    /** True when the value is a structurally valid isikukood (length, century, date, checksum). */
    public static boolean isValid(String raw) {
        String d = digits(raw);
        if (d == null || d.length() != LENGTH) {
            return false;
        }
        return birthDateOf(d).isPresent() && checksum(d) == d.charAt(10) - '0';
    }

    /** Birth date encoded in a valid isikukood. */
    public static Optional<LocalDate> birthDate(String raw) {
        return isValid(raw) ? birthDateOf(digits(raw)) : Optional.empty();
    }

    /** Age on {@code on} derived from a valid isikukood. */
    public static Optional<Integer> ageOn(String raw, LocalDate on) {
        return birthDate(raw).map(d -> Period.between(d, on).getYears());
    }

    // --- internals ---

    private static Optional<LocalDate> birthDateOf(String d) {
        int centuryDigit = d.charAt(0) - '0';
        if (centuryDigit < 1 || centuryDigit > 8) {
            return Optional.empty();
        }
        int century = 1800 + ((centuryDigit - 1) / 2) * 100;
        int year = century + Integer.parseInt(d.substring(1, 3));
        int month = Integer.parseInt(d.substring(3, 5));
        int day = Integer.parseInt(d.substring(5, 7));
        try {
            return Optional.of(LocalDate.of(year, month, day));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    private static int checksum(String d) {
        int mod = weightedMod(d, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 1});
        if (mod < 10) {
            return mod;
        }
        mod = weightedMod(d, new int[] {3, 4, 5, 6, 7, 8, 9, 1, 2, 3});
        return mod < 10 ? mod : 0;
    }

    private static int weightedMod(String d, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (d.charAt(i) - '0') * weights[i];
        }
        return sum % 11;
    }
}
