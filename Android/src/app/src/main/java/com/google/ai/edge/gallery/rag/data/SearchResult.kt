package com.google.ai.edge.gallery.rag.data

data class SearchResult(
    val chunk: PlantChunk,
    val similarityScore: Float,
    val rank: Int
)
