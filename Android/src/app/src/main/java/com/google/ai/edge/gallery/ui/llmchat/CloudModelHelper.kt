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

package com.google.ai.edge.gallery.ui.llmchat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.gallery.api.OpenAiApiService
import com.google.ai.edge.gallery.data.CloudModelInstance
import com.google.ai.edge.gallery.data.Model
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "AGCloudModelHelper"

typealias CloudResultListener = (partialResult: String, done: Boolean) -> Unit
typealias CloudCleanUpListener = () -> Unit

object CloudModelHelper {
  private val apiServices = mutableMapOf<String, OpenAiApiService>()

  fun initialize(
    context: Context,
    model: Model,
    apiKey: String,
    onDone: (String) -> Unit,
  ) {
    try {

      if (model.instance != null) {
          cleanUp(model) { }
      }
      when (model.cloudProvider) {
        "openai" -> {
          val apiService = OpenAiApiService()
          apiServices[model.name] = apiService
          model.instance = CloudModelInstance(
            provider = model.cloudProvider,
            modelName = model.cloudModelName,
            apiKey = apiKey.trim(),
            isConnected = true
          )
        }
        else -> {
          onDone("Unsupported cloud provider: ${model.cloudProvider}")
          return
        }
      }
      onDone("")
    } catch (e: Exception) {
      Log.e(TAG, "Error initializing cloud model", e)
      onDone("Failed to initialize cloud model: ${e.message}")
    }
  }

  fun runInference(
    model: Model,
    input: String,
    resultListener: CloudResultListener,
    cleanUpListener: CloudCleanUpListener,
    images: List<Bitmap> = listOf(),
  ) {
    val instance = model.instance as? CloudModelInstance ?: return
    val apiService = apiServices[model.name] ?: return

    try {
      when (instance.provider) {
        "openai" -> {
          val messages = if (images.isNotEmpty()) {
            apiService.createMessageWithImage(input, images)
          } else {
            // Use text-only message for LLM Single Turn
            listOf(
              com.google.ai.edge.gallery.api.OpenAiMessage(
                role = "user",
                content = listOf(
                  com.google.ai.edge.gallery.api.OpenAiContent(
                    type = "text",
                    text = input
                  )
                )
              )
            )
          }

          // Run streaming inference in a coroutine
          kotlinx.coroutines.runBlocking {
            try {
              apiService.streamChatCompletion(
                apiKey = instance.apiKey,
                model = instance.modelName,
                messages = messages
              ).collect { partialResult ->
                resultListener(partialResult, false)
              }
              resultListener("", true)
            } catch (e: Exception) {
              Log.e(TAG, "Error in stream collection", e)
              resultListener("Error: ${e.message}", true)
            }
          }
        }
        else -> {
          resultListener("Unsupported cloud provider: ${instance.provider}", true)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error running cloud inference", e)
      resultListener("Error: ${e.message}", true)
    }
  }

  fun cleanUp(model: Model, onDone: () -> Unit) {
    apiServices.remove(model.name)
    model.instance = null
    onDone()
  }
}
