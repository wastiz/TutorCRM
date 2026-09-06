package com.tutormgmt.student.importer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmartStudentImportParserTest {

    private final SmartStudentImportParser parser =
            new SmartStudentImportParser(new TabStudentImportParser(), new FreeTextStudentImportParser());

    @Test
    void routesTheCanonicalTabMessageToTheTabParser() {
        String raw = "Kirill Tsarenkov\tljulap@gmail.com\t5350 6894\t14\t9\tTlvl\tЭстонский язык\t"
                + "goal\tЧЕТВЕРГ\t17-20\t2\tоба варианта подходят\t\tLiudmila Lapshina\t\t"
                + "51109180029\t47808060232";

        StudentImportPreviewDto p = parser.parse(raw);

        assertThat(p.fieldCount()).isEqualTo(17);
        assertThat(p.student().school()).isEqualTo("Tlvl");
        assertThat(p.student().subject()).isEqualTo("Эстонский язык");
    }

    @Test
    void routesFreeTextToTheFreeTextParser() {
        StudentImportPreviewDto p = parser.parse("Anna Ivanova anna@mail.ee 58170531");

        assertThat(p.student().firstName()).isEqualTo("Anna");
        assertThat(p.student().email()).isEqualTo("anna@mail.ee");
        assertThat(p.student().phone()).isEqualTo("58170531");
    }

    @Test
    void prefersWhicheverStrategyRecognizesMoreOnAHalfBrokenPaste() {
        StudentImportPreviewDto p = parser.parse("Anna Ivanova\tanna@mail.ee 58170531 Maria Ivanova");

        assertThat(p.student().firstName()).isEqualTo("Anna");
        assertThat(p.student().email()).isEqualTo("anna@mail.ee");
        assertThat(p.student().phone()).isEqualTo("58170531");
    }
}
