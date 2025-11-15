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

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "plant_chunks")
data class PlantChunkRoom(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "chunk_id")
    val chunkId: Int,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "scientific_name")
    val scientificName: String?,

    @ColumnInfo(name = "common_name")
    val commonName: String?,

    @ColumnInfo(name = "family")
    val family: String?,

    @ColumnInfo(name = "genus")
    val genus: String?,

    @ColumnInfo(name = "order")
    val order: String?,

    @ColumnInfo(name = "class")
    val class_: String?,

    @ColumnInfo(name = "phylum")
    val phylum: String?,

    @ColumnInfo(name = "section")
    val section: String,

    @ColumnInfo(name = "length")
    val length: Int,

    @ColumnInfo(name = "word_count")
    val wordCount: Int,

    @ColumnInfo(name = "wikipedia_url")
    val wikipediaUrl: String?,

    @ColumnInfo(name = "categories")
    val categories: String,  // Store as comma-separated string

    @ColumnInfo(name = "summary")
    val summary: String?,

    @ColumnInfo(name = "images")
    val images: String,  // Store as comma-separated string

    @ColumnInfo(name = "error")
    val error: String?,

    @ColumnInfo(name = "embedding")
    val embedding: String  // Store embeddings as comma-separated string
)

