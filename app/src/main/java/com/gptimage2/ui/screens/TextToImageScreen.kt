package com.gptimage2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gptimage2.data.model.ImageSize
import com.gptimage2.data.model.OutputCount
import com.gptimage2.ui.components.ChipSelector
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.GoldDivider
import com.gptimage2.ui.components.GptCard
import com.gptimage2.ui.components.GptTextField
import com.gptimage2.ui.components.SectionLabel
import com.gptimage2.util.ImageCodec
import com.gptimage2.viewmodel.MainViewModel
import com.gptimage2.viewmodel.T2IState

@Composable
fun TextToImageScreen(state: T2IState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GptCard {
                SectionLabel("Prompt")
                Spacer(Modifier.height(8.dp))
                GptTextField(
                    value = state.prompt,
                    onValueChange = viewModel::updateT2IPrompt,
                    label = "描述你想生成的图像",
                    placeholder = "例：a tiger in neon city, ultra detailed",
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
                    onSelect = viewModel::selectT2ISize
                )
                Spacer(Modifier.height(12.dp))
                SectionLabel("数量")
                Spacer(Modifier.height(8.dp))
                ChipSelector(
                    items = OutputCount.values().toList(),
                    selected = state.count,
                    labelOf = { it.label },
                    onSelect = viewModel::selectT2ICount
                )
            }
        }

        item {
            GoldButton(
                text = if (state.isGenerating) "生成中…" else "生成",
                onClick = viewModel::runTextToImage,
                loading = state.isGenerating,
                leadingIcon = Icons.Default.AutoAwesome,
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
            item { EmptyHint("尚未生成，填写提示词后点击「生成」。") }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.latestResults, key = { it.hashCode() }) { b64 ->
                        Base64Thumbnail(b64)
                    }
                }
            }
        }
    }
}

@Composable
fun Base64Thumbnail(base64: String, sideDp: Int = 160) {
    val bmp = remember(base64) { ImageCodec.decodeBitmap(base64, maxSide = 512) }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sideDp.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Text("invalid image", color = MaterialTheme.colorScheme.error)
    }
}
