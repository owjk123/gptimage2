package com.gptimage2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gptimage2.data.model.BUILT_IN_TEMPLATES
import com.gptimage2.data.model.ImageSize
import com.gptimage2.data.model.OutputCount
import com.gptimage2.data.model.PromptTemplate
import com.gptimage2.data.model.Quality
import com.gptimage2.data.model.OutputFormat
import com.gptimage2.ui.components.ChipSelector
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.GoldDivider
import com.gptimage2.ui.components.GoldSlider
import com.gptimage2.ui.components.GroupedChipSelector
import com.gptimage2.ui.components.GptCard
import com.gptimage2.ui.components.GptTextField
import com.gptimage2.ui.components.SectionLabel
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.util.ApiKeyManager
import com.gptimage2.util.ImageCodec
import com.gptimage2.viewmodel.MainViewModel
import com.gptimage2.viewmodel.T2IState

@Composable
fun TextToImageScreen(
    state: T2IState,
    viewModel: MainViewModel,
    context: android.content.Context
) {
    var showTemplateDialog by remember { mutableStateOf(false) }
    var customPromptInput by remember { mutableStateOf("") }
    var showCustomTemplateDialog by remember { mutableStateOf(false) }
    
    val isOfficialModel = remember { 
        ApiKeyManager.loadModel(context).contains("gpt-image-2")
    }
    val customTemplates = remember { ApiKeyManager.getCustomTemplates(context) }
    val allTemplates = remember { BUILT_IN_TEMPLATES + customTemplates }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GptCard {
                SectionLabel("Prompt")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.prompt,
                        onValueChange = viewModel::updateT2IPrompt,
                        modifier = Modifier.weight(1f),
                        label = { Text("描述你想生成的图像", color = GptColors.Muted) },
                        placeholder = { Text("例：a tiger in neon city, ultra detailed", color = GptColors.Muted.copy(alpha = 0.6f)) },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = GptColors.WarmWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GptColors.ChampagneGold,
                            unfocusedBorderColor = GptColors.DimGold,
                            cursorColor = GptColors.ChampagneGold,
                            focusedContainerColor = GptColors.Charcoal,
                            unfocusedContainerColor = GptColors.Charcoal
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "模板",
                        tint = if (state.selectedTemplate != null) GptColors.ChampagneGold else GptColors.Muted,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showTemplateDialog = true }) {
                    Text(
                        text = state.selectedTemplate?.name ?: "选择提示词模板",
                        color = GptColors.ChampagneGold
                    )
                }
            }
        }

        item {
            GptCard {
                SectionLabel("尺寸")
                Spacer(Modifier.height(8.dp))
                GroupedChipSelector(
                    items = ImageSize.ALL,
                    selected = state.size,
                    labelOf = { it.label },
                    groupOf = { it.group.label },
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
            GptCard {
                SectionLabel("质量")
                Spacer(Modifier.height(8.dp))
                ChipSelector(
                    items = Quality.ALL,
                    selected = state.quality,
                    labelOf = { it.label },
                    onSelect = viewModel::selectT2IQuality
                )
                
                if (isOfficialModel) {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("输出格式")
                    Spacer(Modifier.height(8.dp))
                    ChipSelector(
                        items = OutputFormat.ALL,
                        selected = state.outputFormat,
                        labelOf = { it.label },
                        onSelect = viewModel::selectT2IOutputFormat
                    )
                    
                    if (state.outputFormat != OutputFormat.PNG) {
                        Spacer(Modifier.height(12.dp))
                        GoldSlider(
                            value = state.outputCompression.toFloat(),
                            onValueChange = { viewModel.updateT2ICompression(it.toInt()) },
                            valueRange = 1f..100f,
                            valueLabel = "压缩质量",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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

    // Template selection dialog
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("选择模板", color = GptColors.ChampagneGold) },
            containerColor = GptColors.Onyx,
            titleContentColor = GptColors.ChampagneGold,
            textContentColor = GptColors.WarmWhite,
            confirmButton = {
                TextButton(onClick = { 
                    showTemplateDialog = false
                    showCustomTemplateDialog = true
                }) {
                    Text("新建自定义模板", color = GptColors.ChampagneGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("关闭", color = GptColors.Muted)
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTemplates) { template ->
                        val isSelected = state.selectedTemplate?.id == template.id
                        TextButton(
                            onClick = {
                                viewModel.selectT2ITemplate(template)
                                showTemplateDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = template.name,
                                    color = if (isSelected) GptColors.ChampagneGold else GptColors.WarmWhite
                                )
                                Text(
                                    text = template.prefix,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GptColors.Muted
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    // Custom template dialog
    if (showCustomTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTemplateDialog = false },
            title = { Text("新建自定义模板", color = GptColors.ChampagneGold) },
            containerColor = GptColors.Onyx,
            titleContentColor = GptColors.ChampagneGold,
            textContentColor = GptColors.WarmWhite,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customPromptInput.isNotBlank()) {
                            val template = PromptTemplate(
                                name = "自定义模板",
                                prefix = customPromptInput,
                                isBuiltIn = false
                            )
                            ApiKeyManager.saveCustomTemplate(context, template)
                            viewModel.selectT2ITemplate(template)
                            showCustomTemplateDialog = false
                            customPromptInput = ""
                        }
                    }
                ) {
                    Text("保存并使用", color = GptColors.ChampagneGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTemplateDialog = false }) {
                    Text("取消", color = GptColors.Muted)
                }
            },
            text = {
                OutlinedTextField(
                    value = customPromptInput,
                    onValueChange = { customPromptInput = it },
                    label = { Text("模板前缀", color = GptColors.Muted) },
                    placeholder = { Text("例如：Professional portrait of", color = GptColors.Muted.copy(alpha = 0.6f)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = GptColors.WarmWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GptColors.ChampagneGold,
                        unfocusedBorderColor = GptColors.DimGold,
                        cursorColor = GptColors.ChampagneGold,
                        focusedContainerColor = GptColors.Charcoal,
                        unfocusedContainerColor = GptColors.Charcoal
                    )
                )
            }
        )
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
