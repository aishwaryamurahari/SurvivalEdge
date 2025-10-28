package com.google.ai.edge.gallery.rag.embedding

class TextTokenizer {
    fun tokenize(text: String): List<String> {
        // Simple tokenization - kept for potential future use
        // Note: Universal Sentence Encoder doesn't need tokenization
        return text.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    fun tokenizeToIds(text: String, maxLength: Int = 512): IntArray {
        // This method is not used by Universal Sentence Encoder
        // Kept for potential future use with other models
        val tokens = tokenize(text)
        val ids = IntArray(maxLength)

        // Convert tokens to IDs
        // This is a simplified version - real implementation would use vocabulary
        tokens.take(maxLength).forEachIndexed { index, token ->
            ids[index] = token.hashCode() % 30000 // Dummy vocabulary size
        }

        return ids
    }

    // Universal Sentence Encoder processes raw text directly
    fun prepareForUniversalSentenceEncoder(text: String): String {
        // Universal Sentence Encoder can handle raw text
        // Just return the text as-is
        return text
    }
}
