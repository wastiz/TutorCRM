package com.tutormgmt.student.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IsikukoodTest {

    @Test
    void acceptsRealPersonalCodesFromTheOwnerMessages() {
        assertThat(Isikukood.isValid("51109180029")).isTrue();
        assertThat(Isikukood.isValid("47808060232")).isTrue();
        assertThat(Isikukood.isValid("50711217011")).isTrue();
        assertThat(Isikukood.isValid("48303010225")).isTrue();
    }

    @Test
    void rejectsWrongLengthCenturyDateAndChecksum() {
        assertThat(Isikukood.isValid("5110918002")).isFalse();     // 10 digits
        assertThat(Isikukood.isValid("51109180028")).isFalse();    // bad checksum
        assertThat(Isikukood.isValid("91109180029")).isFalse();    // century digit 9
        assertThat(Isikukood.isValid("51113180020")).isFalse();    // month 13
        assertThat(Isikukood.isValid(null)).isFalse();
        assertThat(Isikukood.isValid("phone 5350 6894")).isFalse();
    }

    @Test
    void decodesBirthDateAndAge() {
        assertThat(Isikukood.birthDate("51109180029")).contains(LocalDate.of(2011, 9, 18));
        assertThat(Isikukood.birthDate("47808060232")).contains(LocalDate.of(1978, 8, 6));
        assertThat(Isikukood.ageOn("51109180029", LocalDate.of(2026, 9, 5))).contains(14);
        assertThat(Isikukood.ageOn("51109180029", LocalDate.of(2026, 9, 17))).contains(14);
        assertThat(Isikukood.ageOn("51109180029", LocalDate.of(2026, 9, 19))).contains(15);
    }
}
