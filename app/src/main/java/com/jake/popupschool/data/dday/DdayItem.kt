package com.jake.popupschool.data.dday

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class DdayItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetDateEpochDay: Long
) {
    fun targetDate(): LocalDate = LocalDate.ofEpochDay(targetDateEpochDay)

    companion object {
        fun create(title: String, date: LocalDate): DdayItem =
            DdayItem(title = title, targetDateEpochDay = date.toEpochDay())
    }
}
