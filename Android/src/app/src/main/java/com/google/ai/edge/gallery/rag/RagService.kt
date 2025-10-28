package com.google.ai.edge.gallery.rag

import android.content.Context
import android.util.Log
import com.google.ai.edge.gallery.rag.config.RagConfig
import com.google.ai.edge.gallery.rag.data.RagResponse
import com.google.ai.edge.gallery.rag.utils.LoggingUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RagService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ragSystem: RagSystem? = null
    private var isInitialized = false
    private val logger = LoggingUtils.getLogger(this::class)

    suspend fun initialize() {
        if (isInitialized) return

        try {
            logger.info("Initializing RAG system...")
            val config = RagConfig(context)
            ragSystem = RagSystem(config)
            ragSystem?.initialize()

            // Check if database needs to be built
            val dbFile = java.io.File(config.databasePath)
            if (!dbFile.exists() || dbFile.length() == 0L) {
                Log.d("RAG_SERVICE", "📊 Database doesn't exist or is empty, building from JSON...")
                try {
                    ragSystem?.buildFromJsonData("data/json/all_plants_streaming.json")
                    Log.d("RAG_SERVICE", "✓ Database built successfully")
                } catch (e: Exception) {
                    Log.e("RAG_SERVICE", "✗ Failed to build database from JSON", e)
                    e.printStackTrace()
                }
            } else {
                Log.d("RAG_SERVICE", "📊 Database already exists (${dbFile.length()} bytes)")
            }

            isInitialized = true
            logger.info("RAG system initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize RAG system", e)
            throw e
        }
    }

    suspend fun enhanceQueryWithRAG(userQuery: String): String {
        Log.d("RAG_SERVICE", "🔍 enhanceQueryWithRAG called with: '$userQuery'")

        if (!isInitialized) {
            Log.d("RAG_SERVICE", "⚠️ Not initialized, initializing now...")
            initialize()
        }

        return try {
            Log.d("RAG_SERVICE", "============================================")
            Log.d("RAG_SERVICE", "Querying RAG for: '$userQuery'")

            val ragResponse = ragSystem?.query(userQuery)

            if (ragResponse != null && ragResponse.chunks.isNotEmpty()) {
                // Format RAG context and prepend to user query
                val ragContext = ragResponse.context

                Log.d("RAG_SERVICE", "✓ RAG returned ${ragResponse.chunks.size} relevant chunks")
                Log.d("RAG_SERVICE", "✓ Total results: ${ragResponse.totalResults}")
                Log.d("RAG_SERVICE", "Enhanced input length: ${ragContext.length} chars")

                // Only prepend context if it contains meaningful information
                val hasRelevantInfo = ragContext.isNotBlank() &&
                    !ragContext.contains("No relevant information found", ignoreCase = true)

                if (hasRelevantInfo) {
                    Log.d("RAG_SERVICE", "✓ Prepending RAG context to query")
                    Log.d("RAG_SERVICE", "============================================")
                    "Based on the following plant information:\n\n$ragContext\n\nUser question: $userQuery"
                } else {
                    Log.w("RAG_SERVICE", "⚠️ RAG context is empty or contains no relevant information, using original query")
                    Log.d("RAG_SERVICE", "============================================")
                    userQuery // Don't prepend useless context
                }
            } else {
                Log.w("RAG_SERVICE", "✗ RAG returned no results (chunks: ${ragResponse?.chunks?.size ?: 0})")
                Log.w("RAG_SERVICE", "Falling back to original query without RAG enhancement")
                Log.d("RAG_SERVICE", "============================================")
                userQuery // Return original query if RAG fails
            }
        } catch (e: Exception) {
            Log.e("RAG_SERVICE", "✗ RAG enhancement failed", e)
            e.printStackTrace()
            Log.d("RAG_SERVICE", "============================================")
            userQuery // Return original query on error
        }
    }

    suspend fun query(userQuery: String): RagResponse? {
        if (!isInitialized) {
            initialize()
        }

        return try {
            ragSystem?.query(userQuery)
        } catch (e: Exception) {
            logger.error("RAG query failed", e)
            null
        }
    }

    fun isInitialized(): Boolean = isInitialized

    /**
     * Clean up database by removing duplicate entries.
     * Call this if you see duplicate results in searches.
     *
     * @return true if cleanup was performed, false if no duplicates were found
     */
    suspend fun cleanupDatabase(): Boolean {
        if (!isInitialized) {
            Log.d("RAG_SERVICE", "⚠️ Not initialized, initializing now...")
            initialize()
        }

        return try {
            Log.d("RAG_SERVICE", "🧹 Starting database cleanup...")
            val config = RagConfig(context)
            val database = com.google.ai.edge.gallery.rag.database.PlantDatabase(config)
            database.initialize()
            val cleaned = database.cleanupDatabase()

            if (cleaned) {
                Log.d("RAG_SERVICE", "✓ Database cleanup completed")
            } else {
                Log.d("RAG_SERVICE", "✓ No duplicates found in database")
            }

            cleaned
        } catch (e: Exception) {
            Log.e("RAG_SERVICE", "✗ Database cleanup failed", e)
            logger.error("Database cleanup failed", e)
            false
        }
    }
}
