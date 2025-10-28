package com.google.ai.edge.gallery.rag.utils

import android.util.Log

/**
 * Utility class to extract plant names from user queries.
 * Handles various query formats like "What is X?", "Tell me about X", etc.
 */
object QueryParser {

    private val TAG = "QueryParser"

    // Common question patterns that might contain plant names
    private val questionPatterns = listOf(
        "what is (.+?)[?.]?$",
        "what are (.+?)[?.]?$",
        "tell me about (.+?)[?.]?$",
        "describe (.+?)[?.]?$",
        "information about (.+?)[?.]?$",
        "who is (.+?)[?.]?$",  // For named plants
        "explain (.+?)[?.]?$",
        "i want to know about (.+?)[?.]?$",
        "how to identify (.+?)[?.]?$",
        "how to find (.+?)[?.]?$",
        "how to recognize (.+?)[?.]?$",
        "how to spot (.+?)[?.]?$",
        "(.+?)[?.]? information",
        "(.+?)[?.]? details"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    /**
     * Extracts the plant name from a user query.
     * Handles various query formats like "What is Umbrella Moss?", "Tell me about oak trees", etc.
     *
     * @param query The user's query string
     * @return The extracted plant name, or null if extraction fails
     */
    fun extractPlantName(query: String): String? {
        if (query.isBlank()) {
            Log.d(TAG, "Empty query provided")
            return null
        }

        Log.d(TAG, "Extracting plant name from: '$query'")

        // Try to match against common question patterns
        for (pattern in questionPatterns) {
            val match = pattern.find(query)
            if (match != null && match.groupValues.size > 1) {
                val plantName = match.groupValues[1].trim()
                if (plantName.isNotBlank()) {
                    Log.d(TAG, "Extracted plant name: '$plantName'")
                    return plantName
                }
            }
        }

        // If no pattern matches, try to find capitalized words or quote blocks
        val normalizedQuery = query.trim()

        // Check for quoted plant names (e.g., "What is 'Umbrella Moss'?")
        val quotedPattern = "'([^']+)'|\"([^\"]+)\"".toRegex()
        quotedPattern.find(normalizedQuery)?.let { match ->
            val quoted = match.value.replace(Regex("['\"]"), "")
            if (quoted.isNotBlank() && quoted.length > 2) {
                Log.d(TAG, "Extracted quoted plant name: '$quoted'")
                return quoted
            }
        }

        // Check for capitalized multi-word phrases (likely plant names)
        val capitalizedPattern = "\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)".toRegex()
        capitalizedPattern.find(normalizedQuery)?.let { match ->
            val capitalized = match.value.trim()
            if (capitalized.length > 3) {
                Log.d(TAG, "Extracted capitalized plant name: '$capitalized'")
                return capitalized
            }
        }

        // Fallback: if query is simple (1-4 words), assume it's the plant name
        val words = normalizedQuery.split(Regex("\\s+"))
        if (words.size in 1..4 && normalizedQuery.length < 100) {
            val simpleName = normalizedQuery.trim()
            Log.d(TAG, "Treating entire query as plant name: '$simpleName'")
            return simpleName
        }

        Log.w(TAG, "Could not extract plant name from query")
        return null
    }

    /**
     * Normalizes a plant name for search.
     * Handles case sensitivity, extra spaces, and common variations.
     */
    fun normalizePlantName(plantName: String): String {
        return plantName
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    /**
     * Generates search variations of a plant name.
     * Useful for finding plants with different common names.
     *
     * Example: "Umbrella Moss" -> ["umbrella moss", "umbrella", "moss"]
     */
    fun generateSearchVariations(plantName: String): List<String> {
        val normalized = normalizePlantName(plantName)
        val variations = mutableListOf(normalized)

        val words = normalized.split(" ")
        if (words.size > 1) {
            // Add individual words
            variations.addAll(words.filter { it.length > 3 })  // Only meaningful words
            // Add just the first word
            if (words.isNotEmpty()) {
                variations.add(words[0])
            }
        }

        return variations.distinct()
    }

    /**
     * Checks if a query is likely asking about a specific plant.
     */
    fun isPlantQuery(query: String): Boolean {
        val plantIndicators = listOf(
            "plant", "flower", "tree", "shrub", "moss", "fern", "herb",
            "species", "genus", "family", "wildlife", "nature", "botany"
        )

        val normalizedQuery = query.lowercase()
        return plantIndicators.any { normalizedQuery.contains(it) }
    }
}

