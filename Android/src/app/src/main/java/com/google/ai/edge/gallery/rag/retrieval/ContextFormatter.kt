package com.google.ai.edge.gallery.rag.retrieval

import android.util.Log
import com.google.ai.edge.gallery.rag.data.SearchResult

class ContextFormatter {
    fun formatContext(results: List<SearchResult>, maxLength: Int = 2000): String {
        if (results.isEmpty()) {
            return "No relevant information found."
        }

        Log.d("CONTEXT_FORMATTER", "Formatting ${results.size} chunks for context")

        var formatted = ""
        for (result in results) {
            val chunk = result.chunk
            val entry = "Plant: ${chunk.commonName ?: chunk.scientificName ?: "Unknown"}\n${chunk.text}\n"

            Log.d("CONTEXT_FORMATTER", "Chunk: ${chunk.commonName ?: chunk.scientificName ?: "Unknown"} (${chunk.text.length} chars)")
            Log.d("CONTEXT_FORMATTER", "Text preview: ${chunk.text.take(100)}...")

            if (formatted.length + entry.length > maxLength) {
                break // Stop if we'd exceed max length
            }
            formatted += entry + "\n"
        }

        Log.d("CONTEXT_FORMATTER", "Final formatted length: ${formatted.length} chars")

        return formatted.trim()
    }
}
