package com.jake.popupschool.data.timetable

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ManualTimetableEntry(
    val id: String = UUID.randomUUID().toString(),
    /** 1=Monday .. 7=Sunday, matching java.time.DayOfWeek.value */
    val dayOfWeek: Int,
    val period: Int,
    /** References [TimetableSubject.id]. */
    val subjectId: String
)
