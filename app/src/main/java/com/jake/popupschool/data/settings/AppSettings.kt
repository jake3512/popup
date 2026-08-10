package com.jake.popupschool.data.settings

import com.jake.popupschool.domain.model.SchoolLevel

data class AppSettings(
    val apiKey: String = "",
    val officeCode: String = "",
    val schoolCode: String = "",
    val schoolName: String = "",
    val schoolLevel: SchoolLevel = SchoolLevel.HIGH,
    val grade: String = "",
    val classNum: String = ""
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && officeCode.isNotBlank() && schoolCode.isNotBlank() &&
            grade.isNotBlank() && classNum.isNotBlank()
}
