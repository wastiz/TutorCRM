package com.tutormgmt.lesson;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * One rate per student, several lesson lengths.
 *
 * <p>{@code Student.lessonPrice} is the rate for a standard 60-minute lesson. The price of an
 * actual lesson is that rate multiplied by the duration factor, so the tutor never types a price:
 * <pre>
 *   1 h   → rate × 1
 *   1.5 h → rate × 1.5
 *   2 h   → rate × 2
 * </pre>
 * The result is copied onto the {@link Lesson} and frozen there (CLAUDE.md section 10.4) — later
 * rate changes never rewrite past lessons.
 */
public final class LessonPricing {

    /** A "standard" lesson, the length the student rate is quoted for. */
    public static final int STANDARD_MINUTES = 60;

    private static final BigDecimal STANDARD = BigDecimal.valueOf(STANDARD_MINUTES);

    private LessonPricing() {
    }

    /** Duration ÷ 60 min, e.g. 1.5 for a 90-minute lesson. */
    public static BigDecimal durationFactor(OffsetDateTime start, OffsetDateTime end) {
        return durationFactor(minutesBetween(start, end));
    }

    public static BigDecimal durationFactor(long minutes) {
        return BigDecimal.valueOf(minutes).divide(STANDARD, 4, RoundingMode.HALF_UP);
    }

    /** Rate × duration factor, rounded to cents. Null rate → null price. */
    public static BigDecimal priceFor(BigDecimal ratePerStandardLesson, OffsetDateTime start, OffsetDateTime end) {
        if (ratePerStandardLesson == null) {
            return null;
        }
        return ratePerStandardLesson.multiply(durationFactor(start, end)).setScale(2, RoundingMode.HALF_UP);
    }

    public static long minutesBetween(OffsetDateTime start, OffsetDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }
}
