package com.google.ai.edge.gallery.rag.embedding

import com.google.ai.edge.gallery.rag.config.RagConfig
import com.google.ai.edge.gallery.rag.data.PlantChunk
import com.google.ai.edge.gallery.rag.utils.LoggingUtils
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class OnDeviceEmbeddingGenerator(
    private val config: RagConfig
) {
    private val logger = LoggingUtils.getLogger(this::class)
    private var interpreter: Interpreter? = null
    private val tokenizer = TextTokenizer()
    private val modelLoader = TfliteModelLoader()

    suspend fun initialize() {
        try {
            if (config.context.assets.openFd(config.modelPath).use { it.declaredLength > 0 }) {
                interpreter = modelLoader.loadModelFromAssets(config.context, config.modelPath)
                logger.info("Universal Sentence Encoder loaded successfully from assets")
            } else {
                logger.warn("Universal Sentence Encoder not found in assets at ${config.modelPath}, using dummy embeddings")
            }
        } catch (e: Exception) {
            logger.error("Failed to load Universal Sentence Encoder", e)
            logger.warn("Falling back to dummy embeddings for testing")
        }
    }

    suspend fun generateEmbedding(text: String): FloatArray {
        if (interpreter == null) {
            initialize()
        }

        return if (interpreter != null) {
            try {
                // Universal Sentence Encoder takes raw text strings (no tokenization needed)
                val input = prepareInput(text)
                val output = Array(1) { FloatArray(config.embeddingDimension) }  // 512 dimensions

                interpreter?.run(input, output)
                output[0]
            } catch (e: Exception) {
                logger.error("Failed to generate embedding with Universal Sentence Encoder", e)
                generateDummyEmbedding(text)
            }
        } else {
            generateDummyEmbedding(text)
        }
    }

    suspend fun processChunksForEmbedding(chunks: List<PlantChunk>): Pair<List<PlantChunk>, List<FloatArray>> {
        logger.info("Processing ${chunks.size} chunks for embedding generation")

        val embeddings = mutableListOf<FloatArray>()
        val processedChunks = chunks.mapIndexed { index, chunk ->
            val embedding = generateEmbedding(chunk.text)
            embeddings.add(embedding)
            chunk.copy(embeddingIndex = index)
        }

        return processedChunks to embeddings
    }

    private fun prepareInput(text: String): Array<String> {
        // Universal Sentence Encoder takes raw text strings directly
        // No tokenization or preprocessing needed
        return arrayOf(text)
    }

    private fun generateDummyEmbedding(text: String): FloatArray {
        // Generate a deterministic dummy embedding based on text content
        val embedding = FloatArray(config.embeddingDimension)
        val hash = text.hashCode()

        for (i in embedding.indices) {
            embedding[i] = ((hash + i) % 1000) / 1000.0f - 0.5f
        }

        // Normalize the embedding
        val norm = kotlin.math.sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }.toFloat())
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }

        return embedding
    }
}
