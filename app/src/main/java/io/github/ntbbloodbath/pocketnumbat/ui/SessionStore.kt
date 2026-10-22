package io.github.ntbbloodbath.pocketnumbat.ui

import android.util.AtomicFile
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class SessionStore(directory: File) {
    data class Session(
        val history: List<HistoryEntry> = emptyList(),
        val draft: String = "",
    )

    private val file = AtomicFile(File(directory, FILE_NAME))

    fun load(): Session {
        val root = runCatching {
            JSONObject(String(file.readFully(), Charsets.UTF_8))
        }.getOrNull() ?: return Session()

        val history = root.optJSONArray("history")
            ?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    array.optJSONObject(index)?.toHistoryEntry()
                }
            }
            .orEmpty()
            .takeLast(MAX_HISTORY)
            .mapIndexed { index, entry -> entry.copy(id = index.toLong()) }

        return Session(
            history = history,
            draft = root.optString("draft", ""),
        )
    }

    fun save(history: List<HistoryEntry>, draft: String) {
        val completed = history
            .filter { it.error != null || it.output != null }
            .takeLast(MAX_HISTORY)

        val entries = JSONArray()
        completed.forEach { entry ->
            entries.put(
                JSONObject().apply {
                    put("input", entry.input)
                    entry.error?.let { put("error", it) }
                    entry.output?.let { put("output", it) }
                },
            )
        }

        val root = JSONObject().apply {
            put("version", VERSION)
            put("draft", draft)
            put("history", entries)
        }

        runCatching {
            val stream = file.startWrite()
            try {
                stream.write(root.toString().toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (error: Throwable) {
                file.failWrite(stream)
                throw error
            }
        }
    }

    private fun JSONObject.toHistoryEntry(): HistoryEntry? {
        val input = optString("input", "")
        if (input.isBlank()) return null

        val error = if (has("error") && !isNull("error")) optString("error") else null
        val output = if (has("output") && !isNull("output")) optString("output") else null
        if (error == null && output == null) return null

        return HistoryEntry(id = 0, input = input, output = output, error = error)
    }

    companion object {
        const val MAX_HISTORY = 100
        private const val FILE_NAME = "session.json"
        private const val VERSION = 1
    }
}
