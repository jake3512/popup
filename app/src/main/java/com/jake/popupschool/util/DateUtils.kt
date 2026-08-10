package com.jake.popupschool.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val YMD_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

fun LocalDate.toYmd(): String = format(YMD_FORMATTER)

fun currentAcademicYear(date: LocalDate = LocalDate.now()): Int =
    if (date.monthValue >= 3) date.year else date.year - 1

fun currentSemester(date: LocalDate = LocalDate.now()): Int =
    if (date.monthValue in 3..8) 1 else 2

fun ddayLabel(target: LocalDate, today: LocalDate = LocalDate.now()): String {
    val diff = ChronoUnit.DAYS.between(today, target)
    return when {
        diff == 0L -> "D-DAY"
        diff > 0L -> "D-$diff"
        else -> "D+${-diff}"
    }
}
