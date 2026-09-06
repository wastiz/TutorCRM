package com.tutormgmt.lesson;

/** CLAUDE.md section 35 — tracks Google Calendar synchronisation so a lesson is never lost. */
public enum CalendarSyncStatus {
    /** Not yet pushed to Google Calendar. */
    PENDING,
    /** Mirrored to a Google Calendar event. */
    SYNCED,
    /** Last sync attempt failed; the user can retry. */
    FAILED,
    /** Deliberately not synced (Google not connected / user opted out). */
    DISABLED,
    /**
     * The lesson did not happen (cancelled / no-show) and its calendar event was deleted,
     * so the slot is free again in Google Calendar.
     */
    REMOVED
}
