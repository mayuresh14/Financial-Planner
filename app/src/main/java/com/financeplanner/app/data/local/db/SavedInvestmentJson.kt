package com.financeplanner.app.data.local.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encodes/decodes a calculator's raw input fields as a flat String->String
 * map — one flexible JSON shape shared by every saved-investment type,
 * rather than a Room table (or Kotlin class) per calculator. Each
 * ViewModel decides which of its own fields go in the map; "My Investments"
 * reads them back by the same keys to prefill or summarize.
 */
object SavedInvestmentJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(fields: Map<String, String>): String = json.encodeToString(fields)

    fun decode(raw: String): Map<String, String> =
        runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
}
