package com.jake.popupschool.data.dday

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

private val Context.ddayDataStore by preferencesDataStore(name = "dday_store")

class DdayRepository(private val context: Context) {

    private val listKey = stringPreferencesKey("dday_list")
    private val json = Json { ignoreUnknownKeys = true }

    val itemsFlow: Flow<List<DdayItem>> = context.ddayDataStore.data.map { prefs ->
        val raw = prefs[listKey] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<DdayItem>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun add(item: DdayItem) = saveAll(itemsFlow.first() + item)

    suspend fun remove(id: String) = saveAll(itemsFlow.first().filterNot { it.id == id })

    private suspend fun saveAll(items: List<DdayItem>) {
        context.ddayDataStore.edit { prefs ->
            prefs[listKey] = json.encodeToString(items)
        }
    }
}
