package com.google.ai.edge.gallery.rag.config

import android.content.Context
import java.io.File

data class RagConfig(
    val context: Context,
    val embeddingModel: String = "universal_sentence_encoder",
    val embeddingDimension: Int = 512,
    val chunkSize: Int = 400,
    val chunkOverlap: Int = 50,
    val topK: Int = 3,  // Reduced from 10 to 3 to fit within model token limits
    val similarityThreshold: Float = 0.7f,  // Increased from 0.5f for better result quality
    val batchSize: Int = 500,
    val databasePath: String = File(context.filesDir, "plant_database.db").absolutePath,
    val modelPath: String = "universal_sentence_encoder.tflite",
    val maxContextLength: Int = 2000,  // Limit context to ~500 tokens (assuming ~4 chars per token)
    val metadataSearchEnabled: Boolean = true,
    val hybridSearchEnabled: Boolean = true
)
