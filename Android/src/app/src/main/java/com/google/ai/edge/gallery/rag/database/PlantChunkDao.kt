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
}

