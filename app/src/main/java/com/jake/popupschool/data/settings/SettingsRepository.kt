package com.jake.popupschool.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jake.popupschool.domain.model.SchoolLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val OFFICE_CODE = stringPreferencesKey("office_code")
        val SCHOOL_CODE = stringPreferencesKey("school_code")
        val SCHOOL_NAME = stringPreferencesKey("school_name")
        val SCHOOL_LEVEL = stringPreferencesKey("school_level")
        val GRADE = stringPreferencesKey("grade")
        val CLASS_NUM = stringPreferencesKey("class_num")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            apiKey = prefs[Keys.API_KEY].orEmpty(),
            officeCode = prefs[Keys.OFFICE_CODE].orEmpty(),
            schoolCode = prefs[Keys.SCHOOL_CODE].orEmpty(),
            schoolName = prefs[Keys.SCHOOL_NAME].orEmpty(),
            schoolLevel = prefs[Keys.SCHOOL_LEVEL]?.let { name ->
                runCatching { SchoolLevel.valueOf(name) }.getOrNull()
            } ?: SchoolLevel.HIGH,
            grade = prefs[Keys.GRADE].orEmpty(),
            classNum = prefs[Keys.CLASS_NUM].orEmpty()
        )
    }

    suspend fun current(): AppSettings = settingsFlow.first()

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.API_KEY] = settings.apiKey
            prefs[Keys.OFFICE_CODE] = settings.officeCode
            prefs[Keys.SCHOOL_CODE] = settings.schoolCode
            prefs[Keys.SCHOOL_NAME] = settings.schoolName
            prefs[Keys.SCHOOL_LEVEL] = settings.schoolLevel.name
            prefs[Keys.GRADE] = settings.grade
            prefs[Keys.CLASS_NUM] = settings.classNum
        }
    }
}
