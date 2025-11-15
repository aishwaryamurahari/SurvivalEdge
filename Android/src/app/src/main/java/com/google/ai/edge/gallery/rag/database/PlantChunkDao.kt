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

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantChunkDao {

    @Query("SELECT * FROM plant_chunks")
    suspend fun getAllChunks(): List<PlantChunkRoom>

    @Query("SELECT * FROM plant_chunks WHERE chunk_id = :id LIMIT 1")
    suspend fun getChunkById(id: Int): PlantChunkRoom?

    @Query("SELECT * FROM plant_chunks LIMIT :limit")
    suspend fun getChunks(limit: Int): List<PlantChunkRoom>

    @Insert
    suspend fun insertChunk(chunk: PlantChunkRoom): Long

    @Insert
    suspend fun insertChunks(chunks: List<PlantChunkRoom>)

    @Query("SELECT COUNT(*) FROM plant_chunks")
    suspend fun getChunkCount(): Int

    @Delete
    suspend fun deleteChunk(chunk: PlantChunkRoom)

    @Query("DELETE FROM plant_chunks")
    suspend fun deleteAllChunks()

    @Update
    suspend fun updateChunk(chunk: PlantChunkRoom)

    // New methods for memory-efficient similarity search
    @Query("SELECT chunk_id, embedding FROM plant_chunks LIMIT :limit OFFSET :offset")
    suspend fun getEmbeddingsBatch(limit: Int, offset: Int): List<EmbeddingData>

    @Query("SELECT * FROM plant_chunks WHERE chunk_id IN (:ids)")
    suspend fun getChunksByIds(ids: List<Int>): List<PlantChunkRoom>

    // Metadata-based search methods for plant name recognition
    // NOTE: All queries have LIMIT clauses to prevent OutOfMemoryError
    @Query("SELECT * FROM plant_chunks WHERE common_name LIKE '%' || :plantName || '%' OR scientific_name LIKE '%' || :plantName || '%' LIMIT :limit")
    suspend fun searchByPlantName(plantName: String, limit: Int = 100): List<PlantChunkRoom>

    @Query("SELECT * FROM plant_chunks WHERE LOWER(common_name) LIKE LOWER('%' || :partialName || '%') LIMIT :limit")
    suspend fun searchByPartialName(partialName: String, limit: Int = 100): List<PlantChunkRoom>

    @Query("SELECT * FROM plant_chunks WHERE scientific_name LIKE '%' || :scientificName || '%' LIMIT :limit")
    suspend fun searchByScientificName(scientificName: String, limit: Int = 100): List<PlantChunkRoom>
}

