package com.jake.popupschool.data.timetable

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

private val Context.timetableSubjectDataStore by preferencesDataStore(name = "timetable_subject_store")

class TimetableSubjectRepository(private val context: Context) {

    private val listKey = stringPreferencesKey("subjects")
    private val json = Json { ignoreUnknownKeys = true }

    val subjectsFlow: Flow<List<TimetableSubject>> = context.timetableSubjectDataStore.data.map { prefs ->
        val raw = prefs[listKey] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<TimetableSubject>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun add(subject: TimetableSubject) = saveAll(subjectsFlow.first() + subject)

    suspend fun remove(id: String) = saveAll(subjectsFlow.first().filterNot { it.id == id })

    private suspend fun saveAll(subjects: List<TimetableSubject>) {
        context.timetableSubjectDataStore.edit { prefs ->
            prefs[listKey] = json.encodeToString(subjects)
        }
    }
}
