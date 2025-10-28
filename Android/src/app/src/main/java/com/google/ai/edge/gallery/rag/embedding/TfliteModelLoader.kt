package com.google.ai.edge.gallery.rag.embedding

import android.content.Context
import com.google.ai.edge.gallery.rag.utils.LoggingUtils
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TfliteModelLoader {
    private val logger = LoggingUtils.getLogger(this::class)

    fun loadModel(modelPath: String): Interpreter {
        try {
            val modelBuffer = loadModelFile(modelPath)
            val interpreter = Interpreter(modelBuffer)
            logger.info("Universal Sentence Encoder loaded from $modelPath")
            return interpreter
        } catch (e: Exception) {
            logger.error("Failed to load Universal Sentence Encoder from $modelPath", e)
            throw e
        }
    }

    fun loadModelFromAssets(context: Context, assetPath: String): Interpreter {
        try {
            val modelBuffer = loadModelFileFromAssets(context, assetPath)
            val interpreter = Interpreter(modelBuffer)
            logger.info("Universal Sentence Encoder loaded from assets: $assetPath")
            return interpreter
        } catch (e: Exception) {
            logger.error("Failed to load Universal Sentence Encoder from assets: $assetPath", e)
            throw e
        }
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileInputStream = FileInputStream(modelPath)
        val fileChannel = fileInputStream.channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadModelFileFromAssets(context: Context, assetPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(assetPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
