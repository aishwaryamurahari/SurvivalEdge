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
            val allRoomChunks = plantChunkDao.getAllChunks()
            android.util.Log.d("PlantDatabase", "📊 Total chunks in database: ${allRoomChunks.size}")

            val similarities = allRoomChunks.map { roomChunk ->
                val chunkEmbedding = roomChunk.embedding
                    .split(",")
                    .map { it.toFloat() }
                    .toFloatArray()
                val similarity = calculateCosineSimilarity(queryEmbedding, chunkEmbedding)
                roomChunk to similarity
            }

            android.util.Log.d("PlantDatabase", "📊 Calculated similarities: ${similarities.size}")
            val topSimilarity = similarities.maxByOrNull { it.second }?.second
            android.util.Log.d("PlantDatabase", "📊 Top similarity score: $topSimilarity (threshold: $threshold)")

            val topResults = similarities
                .filter { it.second >= threshold }
                .sortedByDescending { it.second }
                .take(topK)

            android.util.Log.d("PlantDatabase", "📊 Filtered results above threshold: ${topResults.size}")

            topResults.map { (roomChunk, _) ->
                roomChunk.toPlantChunk()
            }
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
