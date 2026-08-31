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
    class RealExamples {

        /** CLAUDE.md section 58. */
        @Test
        void kirillTsarenkov() {
            String raw = "Kirill Tsarenkov\tljulap@gmail.com\t5350 6894\t14\t9\tTlvl\tЭстонский язык\t"
                    + "Слабый, особенно речь,нужна подготовка к экзамену \tЧЕТВЕРГ\t17-20\t2\t"
                    + "оба варианта подходят\t\tLiudmila Lapshina \t\t51109180029\t47808060232";

            StudentImportPreviewDto preview = parser.parse(raw);
            ImportedStudentDto s = preview.student();

            assertThat(preview.fieldCount()).isEqualTo(17);
            assertThat(preview.warnings()).isEmpty();
            assertThat(s.firstName()).isEqualTo("Kirill");
            assertThat(s.lastName()).isEqualTo("Tsarenkov");
            assertThat(s.email()).isEqualTo("ljulap@gmail.com");
            assertThat(s.phone()).isEqualTo("5350 6894");
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
            assertThat(s.parentName()).isEqualTo("Liudmila Lapshina");
            assertThat(s.parentPhone()).isEqualTo("51109180029");
            assertThat(s.parentSecondaryPhone()).isEqualTo("47808060232");
            assertThat(s.schedules()).singleElement().satisfies(e -> {
                assertThat(e.dayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
                assertThat(e.startTime()).isEqualTo("17:00");
                assertThat(e.endTime()).isEqualTo("20:00");
                assertThat(e.lessonsPerWeek()).isEqualTo(2);
            });
        }

        /** CLAUDE.md section 57 — empty time, stray leading comma, "онлайн". */
        @Test
        void artjomZimin() {
            String raw = "Artjom Zimin\tartemzimin171@gmail.com\t58170531\t18\t12\tTTG\tМатематика\t"
                    + "Подготовка к экзаменам в 12 классе.Уровень слабый.\t, ПЯТНИЦА, ВОСКРЕСЕНЬЕ\t\t1\tонлайн\t\t"
                    + "Julia Zimina\t\t50711217011\t48303010225";

            StudentImportPreviewDto preview = parser.parse(raw);
            ImportedStudentDto s = preview.student();

            assertThat(preview.warnings()).isEmpty();
            assertThat(s.firstName()).isEqualTo("Artjom");
            assertThat(s.lastName()).isEqualTo("Zimin");
            assertThat(s.phone()).isEqualTo("58170531");
            assertThat(s.age()).isEqualTo(18);
            assertThat(s.grade()).isEqualTo(12);
            assertThat(s.preferredDays()).containsExactly(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY);
            assertThat(s.preferredTimeFrom()).isNull();
            assertThat(s.preferredTimeTo()).isNull();
            assertThat(s.lessonFormat()).isEqualTo(LessonFormat.ONLINE);
            assertThat(s.lessonsPerWeek()).isEqualTo(1);
            assertThat(s.parentName()).isEqualTo("Julia Zimina");
            assertThat(s.parentPhone()).isEqualTo("50711217011");
            assertThat(s.parentSecondaryPhone()).isEqualTo("48303010225");
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
        String raw = "﻿   Kirill   Tsarenkov  \t  ljulap@gmail.com \t 5350 6894 \t 14 \t 9 \t Tlvl \t"
                + " Эстонский язык \t goal \t ЧЕТВЕРГ \t 17-20 \t 2 \t онлайн \t\t Liudmila \t\t 111 \t 222 ";
        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.firstName()).isEqualTo("Kirill");
        assertThat(s.lastName()).isEqualTo("Tsarenkov");
        assertThat(s.email()).isEqualTo("ljulap@gmail.com");
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
