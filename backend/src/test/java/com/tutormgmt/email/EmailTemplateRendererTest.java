package com.tutormgmt.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void fillsPlaceholdersInSubjectAndBody() {
        EmailTemplateRenderer.Rendered r = renderer.render(
                "Урок {{lesson.date}}",
                "Здравствуйте, {{student.firstName}}! Ждём Вас в {{lesson.time}}.",
                Map.of("lesson.date", "10.09.2026", "lesson.time", "17:00",
                        "student.firstName", "Kirill"));

        assertThat(r.subject()).isEqualTo("Урок 10.09.2026");
        assertThat(r.body()).isEqualTo("Здравствуйте, Kirill! Ждём Вас в 17:00.");
        assertThat(r.unresolvedPlaceholders()).isEmpty();
    }

    @Test
    void toleratesSpacesInsideTheBracesAndRepeatedPlaceholders() {
        EmailTemplateRenderer.Rendered r = renderer.render(
                "{{ student.firstName }}",
                "{{student.firstName}} {{student.firstName}}",
                Map.of("student.firstName", "Anna"));

        assertThat(r.subject()).isEqualTo("Anna");
        assertThat(r.body()).isEqualTo("Anna Anna");
    }

    @Test
    void anEmptyValueIsReportedAndLeftVisibleInsteadOfSendingABlank() {
        EmailTemplateRenderer.Rendered r = renderer.render(
                "Hi {{student.firstName}}",
                "Your parent {{parent.name}} asked for {{unknown.thing}}",
                Map.of("student.firstName", "Anna", "parent.name", ""));

        assertThat(r.body()).contains("{{parent.name}}").contains("{{unknown.thing}}");
        assertThat(r.unresolvedPlaceholders()).containsExactly("parent.name", "unknown.thing");
    }

    @Test
    void listsThePlaceholdersATemplateUses() {
        assertThat(renderer.placeholdersIn("{{a.b}} and {{c}} and {{a.b}}"))
                .containsExactly("a.b", "c");
        assertThat(renderer.placeholdersIn(null)).isEmpty();
    }

    @Test
    void textWithoutPlaceholdersIsUntouched() {
        EmailTemplateRenderer.Rendered r = renderer.render("Subject", "Body", Map.of());
        assertThat(r.subject()).isEqualTo("Subject");
        assertThat(r.body()).isEqualTo("Body");
    }
}
