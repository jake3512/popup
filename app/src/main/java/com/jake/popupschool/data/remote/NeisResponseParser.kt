package com.jake.popupschool.data.remote

import com.google.gson.JsonObject

/**
 * NEIS responses are shaped inconsistently: on success the root object has a key
 * matching the endpoint name whose value is `[{"head": [...]}, {"row": [...]}]`,
 * but on error (or "no data") the root is instead `{"RESULT": {"CODE":..., "MESSAGE":...}}`.
 * This parser normalizes both shapes.
 */
object NeisResponseParser {

    fun parseRows(root: JsonObject, endpointName: String): List<JsonObject> {
        val endpointArray = root.getAsJsonArray(endpointName) ?: return emptyList()
        for (element in endpointArray) {
            val obj = element.asJsonObject
            if (obj.has("row")) {
                return obj.getAsJsonArray("row").map { it.asJsonObject }
            }
        }
        return emptyList()
    }

    fun errorMessage(root: JsonObject): String? {
        val result = root.getAsJsonObject("RESULT") ?: return null
        return result.get("MESSAGE")?.asString
    }
}
