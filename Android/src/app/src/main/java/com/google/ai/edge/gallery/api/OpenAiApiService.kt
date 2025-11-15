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

package com.google.ai.edge.gallery.api

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

data class OpenAiMessage(
  val role: String,
  val content: List<OpenAiContent>
)

data class OpenAiContent(
  val type: String,
  val text: String? = null,
  @SerializedName("image_url") val imageUrl: OpenAiImageUrl? = null
)

data class OpenAiImageUrl(
  val url: String
)

data class OpenAiRequest(
  val model: String,
  val messages: List<OpenAiMessage>,
  val max_tokens: Int = 1000,
  val temperature: Float = 0.7f,
  val stream: Boolean = true
)

data class OpenAiResponse(
  val choices: List<OpenAiChoice>
)

data class OpenAiChoice(
  val delta: OpenAiDelta
)

data class OpenAiDelta(
  val content: String? = null
)

class OpenAiApiService {
  private val client = OkHttpClient()
  private val baseUrl = "https://api.openai.com/v1"

  fun streamChatCompletion(
    apiKey: String,
    model: String,
    messages: List<OpenAiMessage>,
    maxTokens: Int = 1000,
    temperature: Float = 0.7f
  ): Flow<String> = flow {
    val trimmedApiKey = apiKey.trim()
    if (trimmedApiKey.isEmpty() || !trimmedApiKey.startsWith("sk-")) {
        throw Exception("Invalid API key format")
    }

    val requestBody = OpenAiRequest(
      model = model,
      messages = messages,
      max_tokens = maxTokens,
      temperature = temperature,
      stream = true
    ).let { request ->
      com.google.gson.Gson().toJson(request)
    }

    val request = Request.Builder()
      .url("$baseUrl/chat/completions")
      .addHeader("Authorization", "Bearer ${apiKey.trim()}")
      .addHeader("Content-Type", "application/json")
      .post(requestBody.toRequestBody("application/json".toMediaType()))
      .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw Exception("API call failed: ${response.code} ${response.message}")
      }

      response.body?.let { body ->
        body.source().use { source ->
          while (!source.exhausted()) {
            val line = source.readUtf8Line()
            if (line != null && line.startsWith("data: ")) {
              val data = line.substring(6)
              if (data == "[DONE]") break

              try {
                val response = com.google.gson.Gson().fromJson(data, OpenAiResponse::class.java)
                response.choices.firstOrNull()?.delta?.content?.let { content ->
                  emit(content)
                }
              } catch (e: Exception) {
                // Skip malformed JSON
              }
            }
          }
        }
      }
    }
  }

  fun createMessageWithImage(text: String, images: List<Bitmap>): List<OpenAiMessage> {
    val content = mutableListOf<OpenAiContent>()

    // Add text content
    if (text.isNotEmpty()) {
      content.add(OpenAiContent(type = "text", text = text))
    }

    // Add image content
    images.forEach { bitmap ->
      val base64Image = bitmapToBase64(bitmap)
      content.add(
        OpenAiContent(
          type = "image_url",
          imageUrl = OpenAiImageUrl(url = "data:image/jpeg;base64,$base64Image")
        )
      )
    }

    return listOf(
      OpenAiMessage(role = "user", content = content)
    )
  }

  private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
  }
}

