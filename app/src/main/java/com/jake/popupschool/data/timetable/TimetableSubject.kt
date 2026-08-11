package com.jake.popupschool.data.timetable

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TimetableSubject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** True if this subject requires moving to a different room (e.g. 음악실/과학실). */
    val isMovingClass: Boolean = false
)
