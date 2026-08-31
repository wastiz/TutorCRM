package com.tutormgmt.report;

import java.time.YearMonth;

/** Report.md section 3 — sheet title is "{месяц на русском} {две последние цифры года}", e.g. "август 26". */
final class RussianMonths {

    private static final String[] NAMES = {
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
    };

    private RussianMonths() {}

    static String title(YearMonth month) {
        return NAMES[month.getMonthValue() - 1] + " " + String.format("%02d", month.getYear() % 100);
    }
}
