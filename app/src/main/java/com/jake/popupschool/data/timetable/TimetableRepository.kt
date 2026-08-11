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

private val Context.timetableDataStore by preferencesDataStore(name = "timetable_store")

class TimetableRepository(private val context: Context) {

    private val listKey = stringPreferencesKey("timetable_entries")
    private val json = Json { ignoreUnknownKeys = true }

    val entriesFlow: Flow<List<ManualTimetableEntry>> = context.timetableDataStore.data.map { prefs ->
        val raw = prefs[listKey] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<ManualTimetableEntry>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun add(entry: ManualTimetableEntry) = saveAll(entriesFlow.first() + entry)

    suspend fun remove(id: String) = saveAll(entriesFlow.first().filterNot { it.id == id })

    private suspend fun saveAll(entries: List<ManualTimetableEntry>) {
        context.timetableDataStore.edit { prefs ->
            prefs[listKey] = json.encodeToString(entries)
        }
    }
}
