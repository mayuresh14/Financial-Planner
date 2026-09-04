package com.financeplanner.app.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.financeplanner.app.data.local.db.SavedInvestmentJson
import com.financeplanner.app.data.repository.SavedInvestmentRepository
import com.financeplanner.app.domain.model.SavedInvestment
import com.financeplanner.app.ui.common.formatFieldLabel
import com.financeplanner.app.ui.common.formatFieldValue
import com.financeplanner.app.ui.common.machineOnlyFieldKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Exports every saved investment to JSON (full fidelity — every raw field, for a possible
 * future import) and CSV (human-readable — same field labels/formatting shown in the app's
 * own detail sheet), then hands both files to the system share sheet. Everything's local-only
 * (see CLAUDE.md), so this is the only way a user can get their data off the device.
 */
class InvestmentExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SavedInvestmentRepository
) {
    private val json = Json { prettyPrint = true }
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /** Writes both export files to the cache dir and returns a chooser [Intent] ready to
     * launch, or null if there's nothing saved to export. */
    suspend fun buildShareIntent(): Intent? {
        val items = repository.observeAll().first()
        if (items.isEmpty()) return null

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = fileDateFormat.format(Date())

        val jsonFile = File(exportsDir, "moneymint_investments_$stamp.json")
        jsonFile.writeText(json.encodeToString(JsonArray.serializer(), buildJsonArray(items)))

        val csvFile = File(exportsDir, "moneymint_investments_$stamp.csv")
        csvFile.writeText(buildCsv(items))

        val authority = "${context.packageName}.fileprovider"
        val uris = listOf(jsonFile, csvFile).map { FileProvider.getUriForFile(context, authority, it) }

        val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, null)
    }

    private fun buildJsonArray(items: List<SavedInvestment>): JsonArray = JsonArray(
        items.map { item ->
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(item.id),
                    "type" to JsonPrimitive(item.type.name),
                    "customName" to JsonPrimitive(item.customName),
                    "institutionName" to (item.institutionName?.let { JsonPrimitive(it) } ?: JsonNull),
                    "notes" to (item.notes?.let { JsonPrimitive(it) } ?: JsonNull),
                    "lastComputedValue" to (item.lastComputedValue?.let { JsonPrimitive(it) } ?: JsonNull),
                    "createdAt" to JsonPrimitive(isoDateFormat.format(Date(item.createdAt))),
                    "details" to JsonObject(
                        SavedInvestmentJson.decode(item.detailsJson).mapValues { JsonPrimitive(it.value) }
                    )
                )
            )
        }
    )

    private fun buildCsv(items: List<SavedInvestment>): String {
        val header = listOf("Name", "Type", "Institution", "Current Value", "Created On", "Notes", "Details")
        val rows = items.map { item ->
            val fields = SavedInvestmentJson.decode(item.detailsJson)
            val details = fields.filterKeys { it !in machineOnlyFieldKeys }
                .filterValues { it.isNotBlank() }
                .entries.joinToString("; ") { (key, value) -> "${formatFieldLabel(key)}: ${formatFieldValue(value)}" }
            listOf(
                item.customName,
                item.type.name,
                item.institutionName.orEmpty(),
                item.lastComputedValue?.toString().orEmpty(),
                isoDateFormat.format(Date(item.createdAt)),
                item.notes.orEmpty(),
                details
            )
        }
        return (listOf(header) + rows).joinToString("\r\n") { row -> row.joinToString(",") { csvEscape(it) } }
    }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
