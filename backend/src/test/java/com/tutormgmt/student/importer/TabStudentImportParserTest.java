package com.tutormgmt.student.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.student.LessonFormat;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** CLAUDE.md section 56 — the parser is the headline unit under test. */
class TabStudentImportParserTest {

    private final TabStudentImportParser parser = new TabStudentImportParser();

    @Nested
    class SpecExamples {

        /** CLAUDE.md section 58. */
        @Test
        void maksimIvanov() {
            String raw = "Maksim Ivanov\tmaksim.ivanov@example.com\t5555 0101\t14\t9\tTlvl\tЭстонский язык\t"
                    + "Слабый, особенно речь,нужна подготовка к экзамену \tЧЕТВЕРГ\t17-20\t2\t"
                    + "оба варианта подходят\t\tNatalia Ivanova \t\t51109180007\t47808060003";

            StudentImportPreviewDto preview = parser.parse(raw);
            ImportedStudentDto s = preview.student();

            assertThat(preview.fieldCount()).isEqualTo(17);
            assertThat(preview.warnings()).isEmpty();
            assertThat(s.firstName()).isEqualTo("Maksim");
            assertThat(s.lastName()).isEqualTo("Ivanov");
            assertThat(s.email()).isEqualTo("maksim.ivanov@example.com");
            assertThat(s.phone()).isEqualTo("5555 0101");
            assertThat(s.age()).isEqualTo(14);
            assertThat(s.grade()).isEqualTo(9);
            assertThat(s.school()).isEqualTo("Tlvl");
            assertThat(s.subject()).isEqualTo("Эстонский язык");
            assertThat(s.goal()).isEqualTo("Слабый, особенно речь, нужна подготовка к экзамену");
            assertThat(s.preferredDays()).containsExactly(DayOfWeek.THURSDAY);
            assertThat(s.preferredTimeFrom()).isEqualTo("17:00");
            assertThat(s.preferredTimeTo()).isEqualTo("20:00");
            assertThat(s.lessonsPerWeek()).isEqualTo(2);
            assertThat(s.lessonFormat()).isEqualTo(LessonFormat.BOTH);
            assertThat(s.parentName()).isEqualTo("Natalia Ivanova");
            // the two 11-digit values are personal codes, not phone numbers: the student's own
            // (born 2011-09-18 — matches age 14) and the parent's (born 1978-08-06)
            assertThat(s.isikukood()).isEqualTo("51109180007");
            assertThat(s.parentIsikukood()).isEqualTo("47808060003");
            assertThat(s.parentPhone()).isNull();
            assertThat(s.parentSecondaryPhone()).isNull();
            assertThat(s.schedules()).singleElement().satisfies(e -> {
                assertThat(e.dayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
                assertThat(e.startTime()).isEqualTo("17:00");
                assertThat(e.endTime()).isEqualTo("20:00");
                assertThat(e.lessonsPerWeek()).isEqualTo(2);
            });
        }

        /** CLAUDE.md section 57 — empty time, stray leading comma, "онлайн". */
        @Test
        void denisSokolov() {
            String raw = "Denis Sokolov\tdenis.sokolov@example.com\t55550202\t18\t12\tTTG\tМатематика\t"
                    + "Подготовка к экзаменам в 12 классе.Уровень слабый.\t, ПЯТНИЦА, ВОСКРЕСЕНЬЕ\t\t1\tонлайн\t\t"
                    + "Anna Sokolova\t\t50711210000\t48303010007";

            StudentImportPreviewDto preview = parser.parse(raw);
            ImportedStudentDto s = preview.student();

            assertThat(preview.warnings()).isEmpty();
            assertThat(s.firstName()).isEqualTo("Denis");
            assertThat(s.lastName()).isEqualTo("Sokolov");
            assertThat(s.phone()).isEqualTo("55550202");
            assertThat(s.age()).isEqualTo(18);
            assertThat(s.grade()).isEqualTo(12);
            assertThat(s.preferredDays()).containsExactly(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY);
            assertThat(s.preferredTimeFrom()).isNull();
            assertThat(s.preferredTimeTo()).isNull();
            assertThat(s.lessonFormat()).isEqualTo(LessonFormat.ONLINE);
            assertThat(s.lessonsPerWeek()).isEqualTo(1);
            assertThat(s.parentName()).isEqualTo("Anna Sokolova");
            assertThat(s.isikukood()).isEqualTo("50711210000");
            assertThat(s.parentIsikukood()).isEqualTo("48303010007");
            assertThat(s.schedules()).hasSize(2);
        }
    }

    // --- CLAUDE.md section 56, cases 1..10 ---

    @Test
    void case2_emptyFieldsAreNull() {
        String raw = "Ivan Petrov\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.firstName()).isEqualTo("Ivan");
        assertThat(s.lastName()).isEqualTo("Petrov");
        assertThat(s.email()).isNull();
        assertThat(s.phone()).isNull();
        assertThat(s.age()).isNull();
        assertThat(s.grade()).isNull();
        assertThat(s.subject()).isNull();
        assertThat(s.preferredDays()).isEmpty();
        assertThat(s.lessonFormat()).isNull();
    }

    @Test
    void case3_caseInsensitiveDaysAndFormat() {
        String raw = fields16("Anna Ivanova", "a@b.ee", "1", "10", "5", "S", "Math", "goal",
                "ЧеТвЕрГ", "9-10", "1", "ОНЛАЙН", "", "P", "111", "222");
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.preferredDays()).containsExactly(DayOfWeek.THURSDAY);
        assertThat(s.lessonFormat()).isEqualTo(LessonFormat.ONLINE);
    }

    @Test
    void case4_multipleWeekdays() {
        String raw = fields16("A B", "", "", "", "", "", "", "",
                "пятница, воскресенье", "", "", "", "", "", "", "");
        assertThat(parser.parse(raw).student().preferredDays())
                .containsExactly(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void case5_timeFormats() {
        assertThat(timeOf("17-20")).containsExactly("17:00", "20:00");
        assertThat(timeOf("17:00-20:00")).containsExactly("17:00", "20:00");
        assertThat(timeOf("17 - 20")).containsExactly("17:00", "20:00");
        assertThat(timeOf("17:00 - 20:00")).containsExactly("17:00", "20:00");
        assertThat(timeOf("9-10:30")).containsExactly("09:00", "10:30");
    }

    @Test
    void case5_unrecognizedTimeKeptRawWithWarning() {
        String raw = fields16("A B", "", "", "", "", "", "", "", "", "in the evening", "", "", "", "", "", "");
        StudentImportPreviewDto p = parser.parse(raw);

        assertThat(p.student().preferredTimeFrom()).isNull();
        assertThat(p.student().preferredTimeRaw()).isEqualTo("in the evening");
        assertThat(p.warnings()).anyMatch(w -> w.toLowerCase().contains("time"));
    }

    @Test
    void case6_invalidAge() {
        String raw = fields16("A B", "", "", "abc", "9", "", "", "", "", "", "", "", "", "", "", "");
        StudentImportPreviewDto p = parser.parse(raw);

        assertThat(p.student().age()).isNull();
        assertThat(p.student().grade()).isEqualTo(9);
        assertThat(p.warnings()).anyMatch(w -> w.toLowerCase().contains("age"));
    }

    @Test
    void case7_invalidGrade() {
        String raw = fields16("A B", "", "", "14", "nine", "", "", "", "", "", "", "", "", "", "", "");
        StudentImportPreviewDto p = parser.parse(raw);

        assertThat(p.student().grade()).isNull();
        assertThat(p.warnings()).anyMatch(w -> w.toLowerCase().contains("grade"));
    }

    @Test
    void case8_unknownLessonFormat() {
        String raw = fields16("A B", "", "", "", "", "", "", "", "", "", "", "гибрид", "", "", "", "");
        StudentImportPreviewDto p = parser.parse(raw);

        assertThat(p.student().lessonFormat()).isNull();
        assertThat(p.warnings()).anyMatch(w -> w.toLowerCase().contains("format"));
    }

    @Test
    void case9_wrongFieldCountStillPreviews() {
        StudentImportPreviewDto p = parser.parse("Only Five\tfields\there\tare\tpresent");

        assertThat(p.fieldCount()).isEqualTo(5);
        assertThat(p.warnings()).anyMatch(w -> w.contains("expected 16") || w.contains("Unexpected number"));
        assertThat(p.student().firstName()).isEqualTo("Only");
        assertThat(p.student().lastName()).isEqualTo("Five");
    }

    @Test
    void case10_extraWhitespaceAndBom() {
        String raw = "﻿   Maksim   Ivanov  \t  maksim.ivanov@example.com \t 5555 0101 \t 14 \t 9 \t Tlvl \t"
                + " Эстонский язык \t goal \t ЧЕТВЕРГ \t 17-20 \t 2 \t онлайн \t\t Natalia \t\t 111 \t 222 ";
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.firstName()).isEqualTo("Maksim");
        assertThat(s.lastName()).isEqualTo("Ivanov");
        assertThat(s.email()).isEqualTo("maksim.ivanov@example.com");
        assertThat(s.age()).isEqualTo(14);
        assertThat(s.preferredDays()).containsExactly(DayOfWeek.THURSDAY);
    }

    @Test
    void multiPartNameKeepsAllParts() {
        String raw = fields16("Anna Maria van Smith", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "");
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.firstName()).isEqualTo("Anna");
        assertThat(s.lastName()).isEqualTo("Maria van Smith");
    }

    @Test
    void singleTokenNameWarns() {
        StudentImportPreviewDto p = parser.parse(fields16(
                "Cher", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""));

        assertThat(p.student().firstName()).isEqualTo("Cher");
        assertThat(p.student().lastName()).isNull();
        assertThat(p.warnings()).anyMatch(w -> w.toLowerCase().contains("last name"));
    }

    @Test
    void sixteenFieldLayoutReadsParentPhones() {
        String raw = fields16("A B", "", "", "", "", "", "", "", "", "", "", "", "", "Parent Name", "1110000", "2220000");
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.parentName()).isEqualTo("Parent Name");
        assertThat(s.parentPhone()).isEqualTo("1110000");
        assertThat(s.parentSecondaryPhone()).isEqualTo("2220000");
    }

    @Test
    void isikukoodIsRecognizedWhereverItSits() {
        String raw = fields16("A B", "", "51109180007", "", "", "", "", "", "", "", "", "",
                "", "Parent Name", "", "");
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.isikukood()).isEqualTo("51109180007");
        assertThat(s.phone()).isNull();
    }

    @Test
    void ageIsDerivedFromTheIsikukoodWhenMissing() {
        String raw = fields16("A B", "", "51109180007", "", "", "", "", "", "", "", "", "",
                "", "Parent Name", "", "");
        StudentImportPreviewDto p = parser.parse(raw);

        assertThat(p.student().age())
                .isEqualTo(java.time.Period.between(
                        java.time.LocalDate.of(2011, 9, 18), java.time.LocalDate.now()).getYears());
        assertThat(p.warnings()).anyMatch(w -> w.contains("isikukood"));
    }

    @Test
    void gluedEmailAndPhoneAreSeparated() {
        String raw = fields16("Maksim Ivanov", "maksim.ivanov@example.com5555 0101", "", "14", "", "", "", "",
                "", "", "", "", "", "Natalia Ivanova", "", "");
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.email()).isEqualTo("maksim.ivanov@example.com");
        assertThat(s.phone()).isEqualTo("5555 0101");
    }

    @Test
    void phoneGluedToIsikukoodIsSplit() {
        String raw = fields16("A B", "", "5555010151109180007", "", "", "", "", "", "", "", "", "",
                "", "Parent Name", "", "");
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.phone()).isEqualTo("55550101");
        assertThat(s.isikukood()).isEqualTo("51109180007");
    }

    @Test
    void parentEmailIsRecognizedOnTheParentSide() {
        String raw = fields16("A B", "kid@mail.ee", "", "", "", "", "", "", "", "", "", "",
                "", "Parent Name", "mom@mail.ee", "");
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.email()).isEqualTo("kid@mail.ee");
        assertThat(s.parentEmail()).isEqualTo("mom@mail.ee");
    }

    @Test
    void nullInputDoesNotThrow() {
        StudentImportPreviewDto p = parser.parse(null);
        assertThat(p.student().firstName()).isNull();
        assertThat(p.warnings()).isNotEmpty();
    }

    // --- helpers ---

    private String[] timeOf(String token) {
        String raw = fields16("A B", "", "", "", "", "", "", "", "", token, "", "", "", "", "", "");
        ImportedStudentDto s = parser.parse(raw).student();
        return new String[] {s.preferredTimeFrom(), s.preferredTimeTo()};
    }

    /** Builds a 16-field TAB line (section-14 layout). */
    private static String fields16(String... f) {
        assert f.length == 16;
        return String.join("\t", f);
    }
}
