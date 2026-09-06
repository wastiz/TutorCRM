package com.tutormgmt.email;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Fills {@code {{placeholder}}} slots in a template. Pure and deterministic — no I/O,
 * so the whole rendering step is unit-testable without Google or a database.
 *
 * <p>A placeholder that has no value is left untouched and reported, so the Preview screen can
 * warn the tutor instead of silently sending "Hello {{student.firstName}}".
 */
@Component
public class EmailTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    public Rendered render(String subject, String body, Map<String, String> values) {
        Set<String> unresolved = new LinkedHashSet<>();
        String renderedSubject = replace(subject, values, unresolved);
        String renderedBody = replace(body, values, unresolved);
        return new Rendered(renderedSubject, renderedBody, List.copyOf(unresolved));
    }

    /** Placeholder names used by a template, in order of first appearance. */
    public List<String> placeholdersIn(String text) {
        List<String> found = new ArrayList<>();
        if (text == null) {
            return found;
        }
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            if (!found.contains(m.group(1))) {
                found.add(m.group(1));
            }
        }
        return found;
    }

    private static String replace(String text, Map<String, String> values, Set<String> unresolved) {
        if (text == null) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                unresolved.add(key);
                m.appendReplacement(out, Matcher.quoteReplacement(m.group()));
            } else {
                m.appendReplacement(out, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Result of rendering: what would be sent, plus the placeholders that stayed empty. */
    public record Rendered(String subject, String body, List<String> unresolvedPlaceholders) {}
}
