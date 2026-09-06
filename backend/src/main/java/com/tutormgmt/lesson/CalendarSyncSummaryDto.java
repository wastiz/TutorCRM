package com.tutormgmt.lesson;

import java.util.List;

/** Outcome of re-syncing a whole student's lessons with Google Calendar. */
public record CalendarSyncSummaryDto(
        boolean enabled,
        int considered,
        int synced,
        int removed,
        int failed,
        /** Human-readable reasons, one per failed lesson — shown as-is in the UI. */
        List<String> problems
) {}
