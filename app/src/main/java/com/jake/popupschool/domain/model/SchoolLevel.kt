package com.jake.popupschool.domain.model

enum class SchoolLevel(val timetableEndpoint: String, val displayName: String) {
    ELEMENTARY("elsTimetable", "초등학교"),
    MIDDLE("misTimetable", "중학교"),
    HIGH("hisTimetable", "고등학교"),
    SPECIAL("spsTimetable", "특수학교");

    companion object {
        fun fromKindName(name: String): SchoolLevel = when {
            name.contains("초등") -> ELEMENTARY
            name.contains("중") -> MIDDLE
            name.contains("고등") -> HIGH
            else -> SPECIAL
        }
    }
}
