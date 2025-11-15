package com.google.ai.edge.gallery.rag

import android.util.Log
import com.google.ai.edge.gallery.rag.config.RagConfig
import com.google.ai.edge.gallery.rag.data.RagResponse
import com.google.ai.edge.gallery.rag.data.SearchResult
import com.google.ai.edge.gallery.rag.database.PlantDatabase
import com.google.ai.edge.gallery.rag.embedding.OnDeviceEmbeddingGenerator
import com.google.ai.edge.gallery.rag.processing.JsonProcessor
import com.google.ai.edge.gallery.rag.retrieval.ContextFormatter
import com.google.ai.edge.gallery.rag.retrieval.RagRetriever
import com.google.ai.edge.gallery.rag.utils.LoggingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RagSystem(
    private val config: RagConfig
) {
    private val logger = LoggingUtils.getLogger(this::class)

    private val database = PlantDatabase(config)
    private val embeddingGenerator = OnDeviceEmbeddingGenerator(config)
    private val jsonProcessor = JsonProcessor(config)
    private val ragRetriever = RagRetriever(config, database, embeddingGenerator)
    private val contextFormatter = ContextFormatter()

    private var isInitialized = false

    suspend fun initialize() {
        if (isInitialized) return

        try {
            logger.info("Initializing RAG system...")

            embeddingGenerator.initialize()
            database.initialize()

            isInitialized = true
            logger.info("RAG system initialized successfully")

        } catch (e: Exception) {
            logger.error("Failed to initialize RAG system", e)
            throw e
        }
    }

    suspend fun buildFromJsonData(jsonFile: String) {
        if (!isInitialized) {
            initialize()
        }

        try {
            logger.info("Building RAG system from JSON data: $jsonFile")

            // Process JSON to chunks
            val chunks = jsonProcessor.processJsonToChunks(jsonFile)

            // Generate embeddings
            val (processedChunks, embeddings) = embeddingGenerator.processChunksForEmbedding(chunks)

            // Store in database
            database.storeChunksWithEmbeddings(processedChunks, embeddings)

            logger.info("RAG system built successfully with ${chunks.size} chunks")

        } catch (e: Exception) {
            logger.error("Failed to build RAG system from JSON data", e)
            throw e
        }
    }

    suspend fun query(userQuery: String): RagResponse {
        if (!isInitialized) {
            initialize()
        }

        try {
            logger.info("Processing query: $userQuery")
            Log.d("RAG_SYSTEM", "Processing query: $userQuery")

            val results = ragRetriever.retrieveRelevantChunks(userQuery)
            Log.d("RAG_SYSTEM", "Retrieved ${results.size} relevant chunks")

            val context = contextFormatter.formatContext(results, maxLength = config.maxContextLength)
            Log.d("RAG_SYSTEM", "Formatted context length: ${context.length} chars")

            return RagResponse(
                query = userQuery,
                totalResults = results.size,
                chunks = results,
                context = context
            )

        } catch (e: Exception) {
            Log.e("RAG_SYSTEM", "Failed to process query: $userQuery", e)
            logger.error("Failed to process query: $userQuery", e)
            return RagResponse(
                query = userQuery,
                totalResults = 0,
                chunks = emptyList(),
                context = "",
                error = e.message
            )
        }
    }
}
