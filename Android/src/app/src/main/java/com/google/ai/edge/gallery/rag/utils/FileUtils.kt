package com.google.ai.edge.gallery.rag.utils

import android.content.Context
import java.io.File

object FileUtils {
    fun readFile(filePath: String): String {
        return File(filePath).readText()
    }

    fun writeFile(filePath: String, content: String) {
        File(filePath).writeText(content)
    }

    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    fun createDirectory(path: String) {
        File(path).mkdirs()
    }

    // Android-specific methods
    fun fileExists(context: Context, assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readAssetFile(context: Context, assetPath: String): String {
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }
}
