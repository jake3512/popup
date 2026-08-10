package com.jake.popupschool.data.repository

import com.google.gson.JsonObject
import com.jake.popupschool.data.remote.NeisApi
import com.jake.popupschool.data.remote.NeisResponseParser
import com.jake.popupschool.data.settings.AppSettings
import com.jake.popupschool.domain.model.MealInfo
import com.jake.popupschool.domain.model.SchoolLevel
import com.jake.popupschool.domain.model.SchoolSearchResult
import com.jake.popupschool.domain.model.TimetableSlot
import com.jake.popupschool.util.currentAcademicYear
import com.jake.popupschool.util.currentSemester
import com.jake.popupschool.util.toYmd
import java.time.LocalDate

class SchoolDataRepository(private val api: NeisApi) {

    suspend fun getMealsForDate(settings: AppSettings, date: LocalDate): Result<List<MealInfo>> = runCatching {
        val ymd = date.toYmd()
        val root = api.getMeals(settings.apiKey, "json", settings.officeCode, settings.schoolCode, ymd, ymd)
        val rows = NeisResponseParser.parseRows(root, "mealServiceDietInfo")
        rows.map { row -> row.toMealInfo(date) }
    }

    suspend fun getTimetableForDate(settings: AppSettings, date: LocalDate): Result<List<TimetableSlot>> = runCatching {
        val endpoint = settings.schoolLevel.timetableEndpoint
        val root = api.getTimetable(
            endpoint,
            settings.apiKey,
            "json",
            settings.officeCode,
            settings.schoolCode,
            currentAcademicYear(date).toString(),
            currentSemester(date).toString(),
            settings.grade,
            settings.classNum,
            date.toYmd()
        )
        val rows = NeisResponseParser.parseRows(root, endpoint)
        rows.mapNotNull { it.toTimetableSlot() }.sortedBy { it.period }
    }

    suspend fun searchSchools(apiKey: String, schoolName: String): Result<List<SchoolSearchResult>> = runCatching {
        val root = api.searchSchool(apiKey, "json", schoolName)
        val rows = NeisResponseParser.parseRows(root, "schoolInfo")
        rows.map { it.toSchoolSearchResult() }
    }
}

private fun JsonObject.toMealInfo(date: LocalDate): MealInfo {
    val rawMenu = get("DDISH_NM")?.asString.orEmpty()
    val menuItems = rawMenu.split("<br/>")
        .map { it.replace(Regex("\\([^)]*\\)"), "").trim() }
        .filter { it.isNotBlank() }
    return MealInfo(
        date = date,
        mealType = get("MMEAL_SC_NM")?.asString.orEmpty(),
        menuItems = menuItems,
        calorieInfo = get("CAL_INFO")?.asString
    )
}

private fun JsonObject.toTimetableSlot(): TimetableSlot? {
    val period = get("PERIO")?.asString?.toIntOrNull() ?: return null
    return TimetableSlot(period = period, subject = get("ITRT_CNTNT")?.asString.orEmpty())
}

private fun JsonObject.toSchoolSearchResult(): SchoolSearchResult = SchoolSearchResult(
    officeCode = get("ATPT_OFCDC_SC_CODE")?.asString.orEmpty(),
    officeName = get("ATPT_OFCDC_SC_NM")?.asString.orEmpty(),
    schoolCode = get("SD_SCHUL_CODE")?.asString.orEmpty(),
    schoolName = get("SCHUL_NM")?.asString.orEmpty(),
    schoolLevel = SchoolLevel.fromKindName(get("SCHUL_KND_SC_NM")?.asString.orEmpty())
)
