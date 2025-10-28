package com.google.ai.edge.gallery.rag.data

import kotlinx.serialization.Serializable

@Serializable
data class PlantChunk(
    val chunkId: Int,
    val text: String,
    val source: String,
    val scientificName: String?,
    val commonName: String?,
    val family: String?,
    val genus: String?,
    val order: String?,
    val class_: String?,
    val phylum: String?,
    val section: String,
    val length: Int,
    val wordCount: Int,
    val wikipediaUrl: String?,
    val categories: List<String> = emptyList(),
    val summary: String?,
    val images: List<String> = emptyList(),
    val error: String? = null,
    val embeddingIndex: Int? = null
)
