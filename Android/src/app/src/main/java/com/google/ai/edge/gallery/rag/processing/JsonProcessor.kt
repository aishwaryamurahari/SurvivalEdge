package com.google.ai.edge.gallery.rag.processing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.ai.edge.gallery.rag.config.RagConfig
import com.google.ai.edge.gallery.rag.data.PlantChunk
import com.google.ai.edge.gallery.rag.utils.FileUtils
import com.google.ai.edge.gallery.rag.utils.LoggingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JsonProcessor(
    private val config: RagConfig
) {
    private val logger = LoggingUtils.getLogger(this::class)
    private val objectMapper = jacksonObjectMapper()
    private val sectionClassifier = SectionClassifier()

    suspend fun processJsonToChunks(jsonFile: String): List<PlantChunk> {
        return withContext(Dispatchers.IO) {
            try {
                // Use Android context to read from assets
                val jsonContent = FileUtils.readAssetFile(config.context, jsonFile)
                val entries: List<Map<String, Any>> = objectMapper.readValue(jsonContent)

                logger.info("Loaded ${entries.size} entries from $jsonFile")

                val validEntries = filterValidEntries(entries)
                logger.info("Filtered to ${validEntries.size} valid entries")

                val chunks = validEntries.mapIndexed { index, entry ->
                    createChunkFromJsonEntry(entry, index)
                }

                logger.info("Created ${chunks.size} chunks from JSON data")
                chunks
            } catch (e: Exception) {
                logger.error("Failed to process JSON file $jsonFile", e)
                throw e
            }
        }
    }

    private fun filterValidEntries(entries: List<Map<String, Any>>): List<Map<String, Any>> {
        return entries.filter { entry ->
            val error = entry["error"] as? String
            val content = entry["content"] as? String
            val summary = entry["summary"] as? String

            error.isNullOrBlank() && (!content.isNullOrBlank() || !summary.isNullOrBlank())
        }
    }

    private fun createChunkFromJsonEntry(entry: Map<String, Any>, chunkId: Int): PlantChunk {
        val content = entry["content"] as? String ?: ""
        val summary = entry["summary"] as? String ?: ""
        val text = if (content.isNotBlank()) content else summary

        val section = sectionClassifier.determineSection(text)

        return PlantChunk(
            chunkId = chunkId,
            text = text,
            source = "all_plants_streaming.json",
            scientificName = entry["scientific_name"] as? String,
            commonName = entry["common_name"] as? String,
            family = entry["family"] as? String,
            genus = entry["genus"] as? String,
            order = entry["order"] as? String,
            class_ = entry["class"] as? String,
            phylum = entry["phylum"] as? String,
            section = section,
            length = text.length,
            wordCount = text.split("\\s+".toRegex()).size,
            wikipediaUrl = entry["wikipedia_url"] as? String,
            categories = (entry["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            summary = summary.takeIf { it.isNotBlank() },
            images = (entry["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            error = entry["error"] as? String
        )
    }
}
