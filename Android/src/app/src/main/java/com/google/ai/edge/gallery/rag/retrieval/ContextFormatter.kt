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
            Log.d("CONTEXT_FORMATTER", "Entry length before adding: ${entry.length} chars")
            Log.d("CONTEXT_FORMATTER", "Entry preview (first 200 chars): ${entry.take(200)}")
            if (formatted.length + entry.length > maxLength) {
                // If we haven't added anything yet, truncate the first entry
                if (formatted.isEmpty()) {
                    val spaceLeft = maxLength
                    val truncatedText = chunk.text.take(spaceLeft - ("Plant: ${chunk.commonName ?: chunk.scientificName ?: "Unknown"}\n\n".length))
                    formatted = "Plant: ${chunk.commonName ?: chunk.scientificName ?: "Unknown"}\n$truncatedText\n"
                }
                break
            } else {
                formatted += entry + "\n"
            }
            Log.d("CONTEXT_FORMATTER", "Added to formatted. Total length now: ${formatted.length} chars")
        }

        Log.d("CONTEXT_FORMATTER", "Before trim - formatted length: ${formatted.length} chars")
        Log.d("CONTEXT_FORMATTER", "Before trim - formatted preview (first 300 chars): ${formatted.take(300)}")
        Log.d("CONTEXT_FORMATTER", "Final formatted length: ${formatted.length} chars")

        return formatted.trim()
    }
}
