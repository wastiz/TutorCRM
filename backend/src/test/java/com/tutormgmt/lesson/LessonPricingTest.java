package com.tutormgmt.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** One rate per student × the length of the lesson. */
class LessonPricingTest {

    private static final BigDecimal RATE = new BigDecimal("20.00");

    @Test
    void standardLengthsUseTheirFactor() {
        assertThat(price(60)).isEqualByComparingTo("20.00");
        assertThat(price(90)).isEqualByComparingTo("30.00");
        assertThat(price(120)).isEqualByComparingTo("40.00");
    }

    @Test
    void nonStandardLengthsAreRoundedToCents() {
        assertThat(price(45)).isEqualByComparingTo("15.00");
        assertThat(price(50)).isEqualByComparingTo("16.67");
    }

    @Test
    void durationFactorMatchesTheAdvertisedMultipliers() {
        assertThat(LessonPricing.durationFactor(60)).isEqualByComparingTo("1");
        assertThat(LessonPricing.durationFactor(90)).isEqualByComparingTo("1.5");
        assertThat(LessonPricing.durationFactor(120)).isEqualByComparingTo("2");
    }

    @Test
    void withoutARateThereIsNoPrice() {
        assertThat(LessonPricing.priceFor(null, at(10), at(11))).isNull();
    }

    private static BigDecimal price(int minutes) {
        OffsetDateTime start = at(10);
        return LessonPricing.priceFor(RATE, start, start.plusMinutes(minutes));
    }

    private static OffsetDateTime at(int hour) {
        return OffsetDateTime.of(2026, 9, 3, hour, 0, 0, 0, ZoneOffset.UTC);
    }
}
