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

package com.google.ai.edge.gallery.data

import com.google.ai.edge.gallery.proto.CloudApiConfig

data class CloudModelInstance(
  val provider: String,
  val modelName: String,
  val apiKey: String,
  val baseUrl: String = "",
  var isConnected: Boolean = false
)

object CloudModelDefinitions {
  val OPENAI_GPT4_VISION = Model(
    name = "gpt-4o",
    displayName = "GPT-4o",
    info = "OpenAI's GPT-4o Vision model with image understanding capabilities. Requires API key.",
    isCloudModel = true,
    cloudProvider = "openai",
    cloudModelName = "gpt-4o",
    requiresApiKey = true,
    llmSupportImage = true,
    llmSupportAudio = false,
    learnMoreUrl = "https://openai.com/gpt-4o ",
    bestForTaskIds = listOf(BuiltInTaskId.LLM_ASK_IMAGE)
  )

  val CLOUD_MODELS = listOf(OPENAI_GPT4_VISION)
}

