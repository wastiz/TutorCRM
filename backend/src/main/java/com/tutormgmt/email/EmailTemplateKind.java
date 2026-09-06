package com.tutormgmt.email;

/** What a template is written for — the send screen uses it to pick the right context. */
public enum EmailTemplateKind {
    /** Anything not tied to a particular lesson or month. */
    GENERAL,
    /** Welcome / first contact after a student is created. */
    WELCOME,
    /** About one specific lesson (reminder, confirmation, follow-up). */
    LESSON,
    /** Month summary: lessons taken and the amount due. */
    MONTHLY_SUMMARY
}
