package com.jake.popupschool.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jake.popupschool.domain.model.BubbleIconType
import com.jake.popupschool.domain.model.PopupStyle
import com.jake.popupschool.domain.model.SchoolLevel
import com.jake.popupschool.domain.model.TimetableSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Embedded at the user's explicit request as a convenience default so the app is usable
 * immediately after install. Anyone with access to this source (or its git history) can
 * read this key, since it is committed to the repository.
 */
private const val DEFAULT_NEIS_API_KEY = "1fa8eef383494bb4be893637fc40c7f7"

class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val OFFICE_CODE = stringPreferencesKey("office_code")
        val SCHOOL_CODE = stringPreferencesKey("school_code")
        val SCHOOL_NAME = stringPreferencesKey("school_name")
        val SCHOOL_LEVEL = stringPreferencesKey("school_level")
        val GRADE = stringPreferencesKey("grade")
        val CLASS_NUM = stringPreferencesKey("class_num")
        val TIMETABLE_SOURCE = stringPreferencesKey("timetable_source")
        val POPUP_STYLE = stringPreferencesKey("popup_style")
        val BUBBLE_ICON_TYPE = stringPreferencesKey("bubble_icon_type")
        val BUBBLE_ICON_TEXT = stringPreferencesKey("bubble_icon_text")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            apiKey = prefs[Keys.API_KEY]?.takeIf { it.isNotBlank() } ?: DEFAULT_NEIS_API_KEY,
            officeCode = prefs[Keys.OFFICE_CODE].orEmpty(),
            schoolCode = prefs[Keys.SCHOOL_CODE].orEmpty(),
            schoolName = prefs[Keys.SCHOOL_NAME].orEmpty(),
            schoolLevel = prefs[Keys.SCHOOL_LEVEL]?.let { name ->
                runCatching { SchoolLevel.valueOf(name) }.getOrNull()
            } ?: SchoolLevel.HIGH,
            grade = prefs[Keys.GRADE].orEmpty(),
            classNum = prefs[Keys.CLASS_NUM].orEmpty(),
            timetableSource = prefs[Keys.TIMETABLE_SOURCE]?.let { name ->
                runCatching { TimetableSource.valueOf(name) }.getOrNull()
            } ?: TimetableSource.NEIS,
            popupStyle = prefs[Keys.POPUP_STYLE]?.let { name ->
                runCatching { PopupStyle.valueOf(name) }.getOrNull()
            } ?: PopupStyle.DEFAULT,
            bubbleIconType = prefs[Keys.BUBBLE_ICON_TYPE]?.let { name ->
                runCatching { BubbleIconType.valueOf(name) }.getOrNull()
            } ?: BubbleIconType.DEFAULT,
            bubbleIconText = prefs[Keys.BUBBLE_ICON_TEXT].orEmpty()
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
            prefs[Keys.TIMETABLE_SOURCE] = settings.timetableSource.name
            prefs[Keys.POPUP_STYLE] = settings.popupStyle.name
        }
    }

    suspend fun setTimetableSource(source: TimetableSource) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.TIMETABLE_SOURCE] = source.name
        }
    }

    suspend fun setPopupStyle(style: PopupStyle) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.POPUP_STYLE] = style.name
        }
    }

    suspend fun setBubbleIcon(type: BubbleIconType, text: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BUBBLE_ICON_TYPE] = type.name
            prefs[Keys.BUBBLE_ICON_TEXT] = text
        }
    }
}
