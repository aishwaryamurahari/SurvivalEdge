package com.google.ai.edge.gallery.rag.database

import androidx.room.ColumnInfo

// Data class for embedding data only (no text to save memory)
data class EmbeddingData(
    @ColumnInfo(name = "chunk_id")
    val chunkId: Int,

    @ColumnInfo(name = "embedding")
    val embedding: String
)
