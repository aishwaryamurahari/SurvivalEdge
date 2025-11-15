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

package com.google.ai.edge.gallery.ui.common.modelitem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.DownloadAndTryButton
import com.google.ai.edge.gallery.ui.common.MarkdownText
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.customColors

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CloudModelItem(
  model: Model,
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  onModelClicked: (Model) -> Unit,
  onApiKeyClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
) {
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val cloudConfig = modelManagerUiState.cloudApiConfigs.find { it.provider == model.cloudProvider }
  val isConnected = cloudConfig?.isConnected ?: false

  var isExpanded by remember { mutableStateOf(false) }

  val boxModifier =
    modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(size = 12.dp))
      .background(color = MaterialTheme.customColors.taskCardBgColor)
      .clickable(
        onClick = { isExpanded = !isExpanded },
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, radius = 1000.dp),
      )

  Box(modifier = boxModifier) {
    SharedTransitionLayout {
      AnimatedContent(isExpanded, label = "cloud_model_item_transition") { targetState ->
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { isTraversalGroup = true },
          ) {
            // Cloud icon and model name
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                if (isConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = null,
                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Column {
                Text(
                  text = model.displayName.ifEmpty { model.name },
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (isConnected) "Connected" else "Not connected",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Settings button
            Icon(
              Icons.Filled.Settings,
              contentDescription = "API Settings",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier
                .size(20.dp)
                .clickable { onApiKeyClicked(model) }
            )
          }

          // Show description when expanded
          if (targetState && model.info.isNotEmpty()) {
            MarkdownText(
              model.info,
              smallFontSize = true,
              textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Action buttons
          DownloadAndTryButton(
            task = task,
            model = model,
            enabled = true,
            downloadStatus = null, // Cloud models don't have download status
            modelManagerViewModel = modelManagerViewModel,
            onClicked = { onModelClicked(model) },
            onApiKeyClicked = onApiKeyClicked,
            compact = false,
            canShowTryIt = true
          )
        }
      }
    }
  }
}

