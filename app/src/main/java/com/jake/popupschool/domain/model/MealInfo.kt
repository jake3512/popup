package com.jake.popupschool.domain.model

import java.time.LocalDate

data class MealInfo(
    val date: LocalDate,
    val mealType: String,
    val menuItems: List<String>,
    val calorieInfo: String?
)
