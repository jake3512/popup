package com.jake.popupschool.data.study

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.studyTimeDataStore by preferencesDataStore(name = "study_time_store")

/** Per-date study time in seconds, keyed by [LocalDate.toEpochDay]. */
class StudyTimeRepository(private val context: Context) {

    private val listKey = stringPreferencesKey("study_time_records")
    private val json = Json { ignoreUnknownKeys = true }

    val recordsFlow: Flow<List<StudyTimeRecord>> = context.studyTimeDataStore.data.map { prefs ->
        val raw = prefs[listKey] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<StudyTimeRecord>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun getSeconds(date: LocalDate): Long =
        recordsFlow.first().find { it.dateEpochDay == date.toEpochDay() }?.seconds ?: 0L

    suspend fun setSeconds(date: LocalDate, seconds: Long) {
        val current = recordsFlow.first().toMutableList()
        val epochDay = date.toEpochDay()
        val index = current.indexOfFirst { it.dateEpochDay == epochDay }
        val record = StudyTimeRecord(dateEpochDay = epochDay, seconds = seconds)
        if (index >= 0) current[index] = record else current.add(record)
        context.studyTimeDataStore.edit { prefs ->
            prefs[listKey] = json.encodeToString(current)
        }
    }
}
