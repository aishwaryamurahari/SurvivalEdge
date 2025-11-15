package com.google.ai.edge.gallery.rag.utils

import android.util.Log
import mu.KotlinLogging

object LoggingUtils {
    fun getLogger(clazz: Class<*>) = KotlinLogging.logger(clazz.name)
    fun getLogger(clazz: kotlin.reflect.KClass<*>) = KotlinLogging.logger(clazz.qualifiedName ?: clazz.simpleName ?: "Unknown")

    // Android-specific logging
    fun getAndroidLogger(tag: String) = object {
        fun d(message: String) = Log.d(tag, message)
        fun i(message: String) = Log.i(tag, message)
        fun w(message: String) = Log.w(tag, message)
        fun e(message: String, throwable: Throwable? = null) = Log.e(tag, message, throwable)
    }
}
