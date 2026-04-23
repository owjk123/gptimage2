package com.gptimage2.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gptimage2.data.model.EditReference
import com.gptimage2.data.model.ImageSize
import com.gptimage2.data.model.OutputCount
import com.gptimage2.ui.components.ChipSelector
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.GoldDivider
import com.gptimage2.ui.components.GptCard
import com.gptimage2.ui.components.GptTextField
import com.gptimage2.ui.components.OutlineGoldButton
import com.gptimage2.ui.components.SectionLabel
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.util.ImageCodec
import com.gptimage2.viewmodel.EditState
import com.gptimage2.viewmodel.MainViewModel

@Composable
fun ImageEditScreen(state: EditState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val read = ImageCodec.readUri(context, uri)
            if (read == null) {
                viewModel.showToastPublic("无法读取所选图片")
                return@rememberLauncherForActivityResult
            }
            val (bytes, mime) = read
            val preview = ImageCodec.makePreview(bytes)
            val uploadBytes = ImageCodec.makePreview(bytes, maxSide = 1536)
                .let { ImageCodec.base64ToBytes(it) }
            viewModel.addEditReference(
                EditReference(bytes = uploadBytes, mimeType = "image/jpeg", previewBase64 = preview)
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GptCard {
                SectionLabel("参考图（1-4 张）")
                Spacer(Modifier.height(8.dp))
                Text(
                    "在 prompt 中用 \"image 1\"、\"image 2\" 引用各张参考图。",
                    color = GptColors.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                if (state.references.isEmpty()) {
                    EmptyHint("尚未添加参考图")
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.references, key = { it.id }) { ref ->
                            ReferenceThumb(ref, onRemove = { viewModel.removeEditReference(ref.id) })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlineGoldButton(
                    text = "添加参考图",
                    onClick = { picker.launch("image/*") },
                    enabled = state.references.size < 4,
                    leadingIcon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            GptCard {
                SectionLabel("Prompt")
                Spacer(Modifier.height(8.dp))
                GptTextField(
                    value = state.prompt,
                    onValueChange = viewModel::updateEditPrompt,
                    label = "编辑指令",
                    placeholder = "例：merge the style of image 1 with the subject of image 2",
                    minLines = 4
                )
            }
        }

        item {
            GptCard {
                SectionLabel("尺寸")
                Spacer(Modifier.height(8.dp))
                ChipSelector(
                    items = ImageSize.values().toList(),
                    selected = state.size,
                    labelOf = { it.label },
                    onSelect = viewModel::selectEditSize
                )
                Spacer(Modifier.height(12.dp))
                SectionLabel("数量")
                Spacer(Modifier.height(8.dp))
                ChipSelector(
                    items = OutputCount.values().toList(),
                    selected = state.count,
                    labelOf = { it.label },
                    onSelect = viewModel::selectEditCount
                )
            }
        }

        item {
            GoldButton(
                text = if (state.isGenerating) "处理中…" else "编辑",
                onClick = viewModel::runImageEdit,
                loading = state.isGenerating,
                leadingIcon = Icons.Default.Brush,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            GoldDivider()
            Spacer(Modifier.height(8.dp))
            SectionLabel("最近输出")
            Spacer(Modifier.height(8.dp))
        }

        if (state.latestResults.isEmpty()) {
            item { EmptyHint("尚未生成") }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.latestResults, key = { it.hashCode() }) { b64 -> Base64Thumbnail(b64) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceThumb(ref: EditReference, onRemove: () -> Unit) {
    val bmp = remember(ref.id) { ImageCodec.decodeBitmap(ref.previewBase64, maxSide = 256) }
    Box(Modifier.size(88.dp)) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(2.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "移除", tint = GptColors.WarmWhite)
        }
    }
}
