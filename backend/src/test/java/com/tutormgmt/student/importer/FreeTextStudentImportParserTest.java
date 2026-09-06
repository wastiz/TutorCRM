package com.tutormgmt.student.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.student.LessonFormat;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

/** Free-form messages: no TAB layout, arbitrary order, sometimes no separators at all. */
class FreeTextStudentImportParserTest {

    private final FreeTextStudentImportParser parser = new FreeTextStudentImportParser();

    @Test
    void readsAProseMessage() {
        String raw = "Здравствуйте! Ученик Kirill Tsarenkov, 14 лет, 9 класс, "
                + "почта ljulap@gmail.com, телефон 5350 6894, isikukood 51109180029. "
                + "Занятия четверг 17-20, 2 раза в неделю, оба варианта подходят. "
                + "Мама Liudmila Lapshina, тел +372 5110 9180.";

        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.firstName()).isEqualTo("Kirill");
        assertThat(s.lastName()).isEqualTo("Tsarenkov");
        assertThat(s.age()).isEqualTo(14);
        assertThat(s.grade()).isEqualTo(9);
        assertThat(s.email()).isEqualTo("ljulap@gmail.com");
        assertThat(s.phone()).isEqualTo("5350 6894");
        assertThat(s.isikukood()).isEqualTo("51109180029");
        assertThat(s.preferredDays()).containsExactly(DayOfWeek.THURSDAY);
        assertThat(s.preferredTimeFrom()).isEqualTo("17:00");
        assertThat(s.preferredTimeTo()).isEqualTo("20:00");
        assertThat(s.lessonsPerWeek()).isEqualTo(2);
        assertThat(s.lessonFormat()).isEqualTo(LessonFormat.BOTH);
        assertThat(s.parentName()).isEqualTo("Liudmila Lapshina");
        assertThat(s.parentPhone()).isEqualTo("+372 5110 9180");
    }

    @Test
    void readsOneValuePerLine() {
        String raw = String.join("\n",
                "Anna Ivanova",
                "anna@mail.ee",
                "58170531",
                "онлайн",
                "пятница");

        ImportedStudentDto s = parser.parse(raw).student();

        assertThat(s.firstName()).isEqualTo("Anna");
        assertThat(s.lastName()).isEqualTo("Ivanova");
        assertThat(s.email()).isEqualTo("anna@mail.ee");
        assertThat(s.phone()).isEqualTo("58170531");
        assertThat(s.lessonFormat()).isEqualTo(LessonFormat.ONLINE);
        assertThat(s.preferredDays()).containsExactly(DayOfWeek.FRIDAY);
    }

    @Test
    void separatesValuesThatWerePastedWithoutSeparators() {
        ImportedStudentDto s = parser.parse("AnnaIvanova anna@mail.ee58170531 51109180029").student();

        assertThat(s.firstName()).isEqualTo("Anna");
        assertThat(s.lastName()).isEqualTo("Ivanova");
        assertThat(s.email()).isEqualTo("anna@mail.ee");
        assertThat(s.phone()).isEqualTo("58170531");
        assertThat(s.isikukood()).isEqualTo("51109180029");
    }

    @Test
    void recognizesTelegramHandles() {
        assertThat(parser.parse("Anna Ivanova @anna_tutor").student().telegram()).isEqualTo("anna_tutor");
        assertThat(parser.parse("Anna Ivanova https://t.me/anna_tutor").student().telegram())
                .isEqualTo("anna_tutor");
    }

    @Test
    void keepsUnrecognizedTextAsTheGoalAndWarnsAboutTheMissingEmail() {
        StudentImportPreviewDto p = parser.parse("Anna Ivanova нужна подготовка к экзамену");

        assertThat(p.student().goal()).contains("подготовка к экзамену");
        assertThat(p.warnings()).anyMatch(w -> w.toLowerCase().contains("e-mail"));
    }

    @Test
    void emptyInputDoesNotThrow() {
        assertThat(parser.parse("   ").warnings()).isNotEmpty();
        assertThat(parser.parse(null).student().firstName()).isNull();
    }
}
