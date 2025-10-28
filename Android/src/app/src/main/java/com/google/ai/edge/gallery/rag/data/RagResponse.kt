package com.google.ai.edge.gallery.rag.data

data class RagResponse(
    val query: String,
    val totalResults: Int,
    val chunks: List<SearchResult>,
    val context: String,
    val error: String? = null
)
