package com.tutormgmt.student.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Entry point for raw-message import: picks the strategy that fits the pasted text
 * and is what {@link StudentImportService} injects.
 *
 * <p>A message with the usual TAB layout goes to {@link TabStudentImportParser}; anything else
 * (free text, one value per line, values glued together) goes to
 * {@link FreeTextStudentImportParser}. When the TAB layout is incomplete both are tried and the
 * result that recognized more identity fields wins, so a half-broken paste still previews well.
 * Still fully deterministic — no LLM (CLAUDE.md section 25).
 */
@Primary
@Component
@RequiredArgsConstructor
public class SmartStudentImportParser implements StudentImportParser {

    /** Below this many TAB-separated columns the layout is not the canonical one. */
    private static final int TAB_LAYOUT_MIN_COLUMNS = 16;

    private final TabStudentImportParser tabParser;
    private final FreeTextStudentImportParser freeTextParser;

    @Override
    public StudentImportPreviewDto parse(String rawText) {
        String text = ImportValues.normalize(rawText);
        int columns = text.isEmpty() ? 0 : text.split("\t", -1).length;

        if (columns >= TAB_LAYOUT_MIN_COLUMNS) {
            return tabParser.parse(rawText);
        }
        if (columns <= 1) {
            return freeTextParser.parse(rawText);
        }
        StudentImportPreviewDto tabbed = tabParser.parse(rawText);
        StudentImportPreviewDto free = freeTextParser.parse(rawText);
        return score(free) > score(tabbed) ? free : tabbed;
    }

    /** How many identity fields a parse recovered — the tie-breaker between strategies. */
    private static int score(StudentImportPreviewDto preview) {
        ImportedStudentDto s = preview.student();
        int score = 0;
        for (String field : new String[] {s.firstName(), s.lastName(), s.email(),
                s.phone(), s.isikukood(), s.parentName()}) {
            if (field != null && !field.isBlank()) {
                score++;
            }
        }
        return score;
    }
}
