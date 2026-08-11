package com.jake.popupschool.data.study

import kotlinx.serialization.Serializable

@Serializable
data class StudyTimeRecord(
    val dateEpochDay: Long,
    val seconds: Long
)
