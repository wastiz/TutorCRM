package com.tutormgmt.student.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IsikukoodTest {

    @Test
    void acceptsStructurallyValidPersonalCodes() {
        assertThat(Isikukood.isValid("51109180007")).isTrue();
        assertThat(Isikukood.isValid("47808060003")).isTrue();
        assertThat(Isikukood.isValid("50711210000")).isTrue();
        assertThat(Isikukood.isValid("48303010007")).isTrue();
    }

    @Test
    void rejectsWrongLengthCenturyDateAndChecksum() {
        assertThat(Isikukood.isValid("5110918000")).isFalse();     // 10 digits
        assertThat(Isikukood.isValid("51109180008")).isFalse();    // bad checksum
        assertThat(Isikukood.isValid("91109180007")).isFalse();    // century digit 9
        assertThat(Isikukood.isValid("51113180000")).isFalse();    // month 13
        assertThat(Isikukood.isValid(null)).isFalse();
        assertThat(Isikukood.isValid("phone 5555 0101")).isFalse();
    }

    @Test
    void decodesBirthDateAndAge() {
        assertThat(Isikukood.birthDate("51109180007")).contains(LocalDate.of(2011, 9, 18));
        assertThat(Isikukood.birthDate("47808060003")).contains(LocalDate.of(1978, 8, 6));
        assertThat(Isikukood.ageOn("51109180007", LocalDate.of(2026, 9, 5))).contains(14);
        assertThat(Isikukood.ageOn("51109180007", LocalDate.of(2026, 9, 17))).contains(14);
        assertThat(Isikukood.ageOn("51109180007", LocalDate.of(2026, 9, 19))).contains(15);
    }
}
