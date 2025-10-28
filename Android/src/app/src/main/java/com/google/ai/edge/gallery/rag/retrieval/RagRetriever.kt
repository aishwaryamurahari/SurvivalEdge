package com.google.ai.edge.gallery.rag.retrieval

import android.util.Log
import com.google.ai.edge.gallery.rag.config.RagConfig
import com.google.ai.edge.gallery.rag.data.PlantChunk
import com.google.ai.edge.gallery.rag.data.SearchResult
import com.google.ai.edge.gallery.rag.database.PlantDatabase
import com.google.ai.edge.gallery.rag.embedding.OnDeviceEmbeddingGenerator
import com.google.ai.edge.gallery.rag.utils.LoggingUtils
import com.google.ai.edge.gallery.rag.utils.QueryParser

class RagRetriever(
    private val config: RagConfig,
    private val database: PlantDatabase,
    private val embeddingGenerator: OnDeviceEmbeddingGenerator
) {
    private val logger = LoggingUtils.getLogger(this::class)

    suspend fun retrieveRelevantChunks(query: String): List<SearchResult> {
        try {
            logger.info("Processing query: $query")
            Log.d("RAG_RETRIEVER", "Generating embedding for query: $query")

            // STEP 1: Extract plant name from query
            val plantName = QueryParser.extractPlantName(query)
            Log.d("RAG_RETRIEVER", "Extracted plant name: $plantName")

            // STEP 2: Generate embedding for semantic search fallback
            val queryEmbedding = embeddingGenerator.generateEmbedding(query)
            Log.d("RAG_RETRIEVER", "Embedding generated: ${queryEmbedding.size} dimensions")

            Log.d("RAG_RETRIEVER", "Searching database (topK=${config.topK}, threshold=${config.similarityThreshold})")

            // STEP 3: Use hybrid search - metadata first, semantic fallback
            val similarChunks = database.hybridSearch(
                plantName = plantName,
                queryEmbedding = queryEmbedding,
                topK = config.topK,
                threshold = config.similarityThreshold
            )

            Log.d("RAG_RETRIEVER", "Found ${similarChunks.size} relevant chunks from database")

            val results = similarChunks.mapIndexed { index, chunk ->
                val similarity = calculateSimilarity(queryEmbedding, chunk)

                Log.d("RAG_RETRIEVER", "--- Chunk ${index + 1} ---")
                Log.d("RAG_RETRIEVER", "Common Name: ${chunk.commonName ?: "N/A"}")
                Log.d("RAG_RETRIEVER", "Scientific Name: ${chunk.scientificName ?: "N/A"}")
                Log.d("RAG_RETRIEVER", "Text: ${chunk.text.take(150)}...")

                SearchResult(
                    chunk = chunk,
                    similarityScore = similarity,
                    rank = index + 1
                )
            }

            logger.info("Found ${results.size} relevant chunks for query: $query")
            Log.d("RAG_RETRIEVER", "Formatted ${results.size} results for query: $query")
            return results

        } catch (e: Exception) {
            Log.e("RAG_RETRIEVER", "Failed to retrieve chunks for query: $query", e)
            logger.error("Failed to retrieve chunks for query: $query", e)
            throw e
        }
    }

    private fun calculateSimilarity(queryEmbedding: FloatArray, chunk: PlantChunk): Float {
        // This would need the chunk's embedding from the database
        // For now, return a dummy similarity score
        return 0.8f
    }
}
