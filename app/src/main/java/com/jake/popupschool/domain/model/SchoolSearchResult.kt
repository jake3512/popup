package com.jake.popupschool.domain.model

data class SchoolSearchResult(
    val officeCode: String,
    val officeName: String,
    val schoolCode: String,
    val schoolName: String,
    val schoolLevel: SchoolLevel
)
