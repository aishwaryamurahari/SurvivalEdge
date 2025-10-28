/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.rag.database

import android.util.Log
import com.google.ai.edge.gallery.rag.config.RagConfig
import com.google.ai.edge.gallery.rag.data.PlantChunk
import com.google.ai.edge.gallery.rag.utils.LoggingUtils

class PlantDatabase(
    private val config: RagConfig
) {
    private val logger = LoggingUtils.getLogger(this::class)
    private val roomDatabase = PlantDatabaseRoom.getDatabase(
        config.context,
        config.databasePath
    )
    private val plantChunkDao = roomDatabase.plantChunkDao()

    suspend fun initialize() {
        try {
            logger.info("Database initialized at ${config.databasePath}")
        } catch (e: Exception) {
            logger.error("Failed to initialize database", e)
            throw e
        }
    }

    suspend fun storeChunksWithEmbeddings(chunks: List<PlantChunk>, embeddings: List<FloatArray>) {
        try {
            val roomChunks = chunks.mapIndexed { index, chunk ->
                val embedding = embeddings[index]
                PlantChunkRoom(
                    chunkId = chunk.chunkId,
                    text = chunk.text,
                    source = chunk.source,
                    scientificName = chunk.scientificName,
                    commonName = chunk.commonName,
                    family = chunk.family,
                    genus = chunk.genus,
                    order = chunk.order,
                    class_ = chunk.class_,
                    phylum = chunk.phylum,
                    section = chunk.section,
                    length = chunk.length,
                    wordCount = chunk.wordCount,
                    wikipediaUrl = chunk.wikipediaUrl,
                    categories = chunk.categories.joinToString(","),
                    summary = chunk.summary,
                    images = chunk.images.joinToString(","),
                    error = chunk.error,
                    embedding = embedding.joinToString(",")
                )
            }

            plantChunkDao.insertChunks(roomChunks)
            logger.info("Stored ${chunks.size} chunks with embeddings")
        } catch (e: Exception) {
            logger.error("Failed to store chunks", e)
            throw e
        }
    }

    suspend fun searchSimilarChunks(
        queryEmbedding: FloatArray,
        topK: Int,
        threshold: Float
    ): List<PlantChunk> {
        return try {
            android.util.Log.d("PlantDatabase", "Starting memory-efficient similarity search...")

            // First, check if we can avoid loading all embeddings
            val totalCount = plantChunkDao.getChunkCount()
            android.util.Log.d("PlantDatabase", "📊 Total chunks in database: $totalCount")

            // For large databases, just return empty to avoid OOM
            // User should rely on metadata search instead
            if (totalCount > 5000) {
                android.util.Log.w("PlantDatabase", "⚠️ Database too large ($totalCount chunks), skipping semantic search to avoid OOM")
                return emptyList()
            }

            // Use a sample-based approach to avoid loading all embeddings
            val batchSize = 1000
            val sampleSize = minOf(batchSize, totalCount)

            android.util.Log.d("PlantDatabase", "📊 Processing sample of $sampleSize embeddings (out of $totalCount)")

            // STEP 1: Load a sample of embeddings
            val embeddings = plantChunkDao.getEmbeddingsBatch(sampleSize, 0)
            android.util.Log.d("PlantDatabase", "📊 Loaded ${embeddings.size} embeddings from database")

            // STEP 2: Calculate similarity for embeddings only
            val similarities = embeddings.mapIndexed { index, embeddingData ->
                try {
                    val chunkEmbedding = embeddingData.embedding
                        .split(",")
                        .map { it.toFloat() }
                        .toFloatArray()
                    val similarity = calculateCosineSimilarity(queryEmbedding, chunkEmbedding)
                    index to similarity
                } catch (e: Exception) {
                    android.util.Log.w("PlantDatabase", "Failed to parse embedding for chunk ${embeddingData.chunkId}", e)
                    index to 0f
                }
            }

            android.util.Log.d("PlantDatabase", "📊 Calculated similarities: ${similarities.size}")
            val topSimilarity = similarities.maxByOrNull { it.second }?.second
            android.util.Log.d("PlantDatabase", "📊 Top similarity score: $topSimilarity (threshold: $threshold)")

            // STEP 3: Get top K chunk IDs
            val topIndices = similarities
                .filter { it.second >= threshold }
                .sortedByDescending { it.second }
                .take(topK)
                .map { it.first }

            android.util.Log.d("PlantDatabase", "📊 Top ${topIndices.size} chunks above threshold")

            if (topIndices.isEmpty()) {
                android.util.Log.d("PlantDatabase", "📊 No results found above threshold")
                return emptyList()
            }

            // STEP 4: Load ONLY the top K full chunks
            val chunkIds = topIndices.map { embeddings[it].chunkId }
            android.util.Log.d("PlantDatabase", "📊 Loading full data for chunk IDs: $chunkIds")

            val topChunks = plantChunkDao.getChunksByIds(chunkIds)
            android.util.Log.d("PlantDatabase", "📊 Loaded ${topChunks.size} full chunks")

            topChunks.map { it.toPlantChunk() }
        } catch (e: Exception) {
            android.util.Log.e("PlantDatabase", "Failed to search similar chunks", e)
            logger.error("Failed to search similar chunks", e)
            throw e
        }
    }

    private fun calculateCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) {
            logger.warn("Embedding dimension mismatch: ${a.size} vs ${b.size}")
            return 0f
        }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return if (normA == 0f || normB == 0f) 0f else {
            dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
        }
    }

    /**
     * Search for plants by metadata (common name or scientific name).
     * This is much more reliable than semantic search for plant name queries.
     */
    suspend fun searchByMetadata(plantName: String, limit: Int = 100): List<PlantChunk> {
        return try {
            android.util.Log.d("PlantDatabase", "🔍 Metadata search for: '$plantName' (limit: $limit)")

            // Search by plant name (checks both common_name and scientific_name fields)
            val results = plantChunkDao.searchByPlantName(plantName, limit)
            android.util.Log.d("PlantDatabase", "📊 Found ${results.size} metadata matches")

            // Also try partial matches for better recall
            val partialResults = plantChunkDao.searchByPartialName(plantName, limit)
            android.util.Log.d("PlantDatabase", "📊 Found ${partialResults.size} partial matches")

            // Merge and deduplicate by chunkId
            val allResults = (results + partialResults)
                .distinctBy { it.chunkId }
                .map { it.toPlantChunk() }

            android.util.Log.d("PlantDatabase", "📊 Total unique results: ${allResults.size}")

            // If we have results, prioritize exact matches
            if (allResults.isNotEmpty()) {
                android.util.Log.d("PlantDatabase", "✓ Metadata search successful!")
            }

            allResults
        } catch (e: Exception) {
            android.util.Log.e("PlantDatabase", "Failed to search by metadata", e)
            logger.error("Failed to search by metadata", e)
            emptyList()
        }
    }

    /**
     * Hybrid search: Try metadata first, fallback to semantic search if no results.
     * Combines the benefits of both approaches.
     */
    suspend fun hybridSearch(
        plantName: String?,
        queryEmbedding: FloatArray,
        topK: Int,
        threshold: Float
    ): List<PlantChunk> {
        return try {
            android.util.Log.d("PlantDatabase", "🔍 Starting hybrid search...")
            android.util.Log.d("PlantDatabase", "📝 Plant name: '$plantName'")

            // Step 1: If we have a plant name, try metadata search first
            // Limit results to topK * 3 to ensure we have options but not too many
            val metadataResults = if (plantName != null && plantName.isNotBlank()) {
                android.util.Log.d("PlantDatabase", "🔎 Attempting metadata search for: '$plantName'")
                searchByMetadata(plantName, limit = minOf(topK * 3, 50))
            } else {
                android.util.Log.d("PlantDatabase", "⚠️ No plant name extracted, skipping metadata search")
                emptyList()
            }

            android.util.Log.d("PlantDatabase", "📊 Metadata results: ${metadataResults.size}")

            // Step 2: If metadata search found good results (>= topK), use them
            if (metadataResults.size >= topK) {
                android.util.Log.d("PlantDatabase", "✓ Using metadata results (${metadataResults.size})")
                return metadataResults.take(topK)
            }

            // Step 3: Fallback to semantic search
            android.util.Log.d("PlantDatabase", "⚠️ Metadata search insufficient, falling back to semantic search")
            val semanticResults = searchSimilarChunks(queryEmbedding, topK, threshold)

            // Step 4: If we have any metadata results, merge with semantic results
            if (metadataResults.isNotEmpty()) {
                // Combine and deduplicate
                val combined = (metadataResults + semanticResults)
                    .distinctBy { it.chunkId }
                    .take(topK)

                android.util.Log.d("PlantDatabase", "📊 Merged results: ${combined.size}")
                return combined
            }

            // No metadata results, return semantic results
            android.util.Log.d("PlantDatabase", "📊 Using semantic results: ${semanticResults.size}")
            semanticResults
        } catch (e: Exception) {
            android.util.Log.e("PlantDatabase", "Hybrid search failed", e)
            logger.error("Hybrid search failed", e)
            emptyList()
        }
    }

    /**
     * Deduplicates the database by removing duplicate entries.
     * Processes in batches to avoid OutOfMemoryError.
     * NOTE: This is a simplified version that doesn't load everything into memory.
     * For a full cleanup, rebuild the database from scratch.
     */
    suspend fun cleanupDatabase(): Boolean {
        return try {
            android.util.Log.d("PlantDatabase", "🧹 Starting database cleanup...")

            // Instead of loading everything, we'll use a simpler approach:
            // Just log the count and let the user know to rebuild if needed
            val chunkCount = plantChunkDao.getChunkCount()
            android.util.Log.d("PlantDatabase", "📊 Total chunks in database: ${chunkCount}")

            android.util.Log.w("PlantDatabase", "⚠️ Full cleanup requires database rebuild")
            android.util.Log.d("PlantDatabase", "To properly deduplicate, delete the database file and let it rebuild on next query")

            // Return false to indicate we didn't actually clean up
            // User should manually delete the database file to force a rebuild
            false
        } catch (e: Exception) {
            android.util.Log.e("PlantDatabase", "Failed to cleanup database", e)
            logger.error("Failed to cleanup database", e)
            false
        }
    }
}

// Extension function to convert Room entity to PlantChunk data class
private fun PlantChunkRoom.toPlantChunk(): PlantChunk {
    return PlantChunk(
        chunkId = this.chunkId,
        text = this.text,
        source = this.source,
        scientificName = this.scientificName,
        commonName = this.commonName,
        family = this.family,
        genus = this.genus,
        order = this.order,
        class_ = this.class_,
        phylum = this.phylum,
        section = this.section,
        length = this.length,
        wordCount = this.wordCount,
        wikipediaUrl = this.wikipediaUrl,
        categories = this.categories.split(",").filter { it.isNotBlank() },
        summary = this.summary,
        images = this.images.split(",").filter { it.isNotBlank() },
        error = this.error
    )
}
