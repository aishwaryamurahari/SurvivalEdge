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

package com.google.ai.edge.gallery.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.data.Model

@Composable
fun ApiKeyDialog(
  model: Model?,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var apiKey by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  if (model != null) {
    AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text("Connect to ${model.displayName}")
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "Enter your API key to use ${model.displayName}",
            style = MaterialTheme.typography.bodyMedium
          )
          OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (apiKey.isNotBlank()) {
              onSave(apiKey)
              onDismiss()
            }
          },
          enabled = apiKey.isNotBlank()
        ) {
          Text("Connect")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      },
      modifier = modifier
    )
  }
}

