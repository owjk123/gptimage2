package com.gptimage2.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gptimage2.data.model.ChatImage
import com.gptimage2.data.model.ChatMessage
import com.gptimage2.data.model.ChatRole
import com.gptimage2.data.model.ImageSize
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GptTextField
import com.gptimage2.ui.components.IconActionButton
import com.gptimage2.ui.components.OutlineGoldButton
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.util.ImageCodec
import com.gptimage2.viewmodel.CHAT_ASPECT_PRESETS
import com.gptimage2.viewmodel.ChatAspectPreset
import com.gptimage2.viewmodel.ChatState
import com.gptimage2.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun ChatScreen(state: ChatState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var previewImage by remember { mutableStateOf<ChatImage?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val read = ImageCodec.readUri(context, uri)
            if (read == null) {
                viewModel.showToastPublic("无法读取所选图片")
                return@rememberLauncherForActivityResult
            }
            val b64 = ImageCodec.makePreview(read.first, maxSide = 1024)
            viewModel.addChatImage(ChatImage(base64 = b64, mimeType = "image/jpeg"))
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Spacer(Modifier.weight(1f))
            IconActionButton(
                icon = Icons.Default.Refresh,
                contentDescription = "清空对话",
                onClick = viewModel::resetChat
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.messages.isEmpty()) {
                EmptyHint("发送文字或图片开始多模态对话")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        MessageBubble(msg, onImageTap = { previewImage = it })
                    }
                }
            }
        }

        ChatComposer(
            state = state,
            viewModel = viewModel,
            onPick = { picker.launch("image/*") }
        )
    }

    previewImage?.let { img ->
        FullScreenPreview(
            img = img,
            onClose = { previewImage = null },
            onDownload = {
                // 保存到临时文件并通知
                val bytes = ImageCodec.base64ToBytes(img.base64)
                val file = File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { it.write(bytes) }
                viewModel.showToastPublic("图片已保存到缓存: ${file.name}")
            }
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, onImageTap: (ChatImage) -> Unit) {
    val isUser = msg.role == ChatRole.USER
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    Box(Modifier.fillMaxWidth(), contentAlignment = align) {
        Surface(
            color = if (isUser) GptColors.Steel else GptColors.Onyx,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.heightIn(min = 40.dp)
        ) {
            Column(Modifier.padding(10.dp)) {
                if (msg.images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(msg.images, key = { it.base64.hashCode() }) { img ->
                            BubbleImage(img, onTap = { onImageTap(img) })
                        }
                    }
                    if (msg.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (msg.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = GptColors.ChampagneGold,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("思考中…", color = GptColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (msg.errorMessage != null) {
                    SelectionContainer {
                        Text(
                            "错误: ${msg.errorMessage}",
                            color = GptColors.Error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (msg.text.isNotBlank()) {
                    SelectionContainer {
                        Text(msg.text, color = GptColors.WarmWhite, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleImage(img: ChatImage, onTap: () -> Unit) {
    val bmp = remember(img.base64) { ImageCodec.decodeBitmap(img.base64, maxSide = 512) }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onTap)
        )
    }
}

@Composable
private fun FullScreenPreview(img: ChatImage, onClose: () -> Unit, onDownload: () -> Unit) {
    val bmp = remember(img.base64) { ImageCodec.decodeBitmap(img.base64, maxSide = 4096) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(GptColors.Obsidian),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
            // 顶部工具栏
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "下载", tint = GptColors.ChampagneGold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = GptColors.WarmWhite)
                }
            }
        }
    }
}

@Composable
private fun AspectPresetRow(selected: ChatAspectPreset?, onToggle: (ChatAspectPreset) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(CHAT_ASPECT_PRESETS, key = { it.label }) { preset ->
            val isSelected = selected?.label == preset.label
            Surface(
                onClick = { onToggle(preset) },
                color = if (isSelected) GptColors.ChampagneGold else GptColors.Charcoal,
                contentColor = if (isSelected) GptColors.Obsidian else GptColors.Muted,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isSelected) GptColors.ChampagneGold else GptColors.Steel)
            ) {
                Text(
                    preset.label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ChatComposer(state: ChatState, viewModel: MainViewModel, onPick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(GptColors.Onyx)
            .padding(12.dp)
    ) {
        AspectPresetRow(selected = state.aspectPreset, onToggle = viewModel::toggleAspectPreset)
        Spacer(Modifier.height(6.dp))
        SizePresetRow(selected = state.sizePreset, onSelect = viewModel::selectChatSize)

        if (state.aspectPreset != null) {
            Text(
                "将在提示词前加入：「${state.aspectPreset.prefix}」",
                color = GptColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (state.pendingImages.isNotEmpty()) {
            Text(
                "点击下方缩略图把 image 1 / image 2 … 自动加进提示词",
                color = GptColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(state.pendingImages.size) { idx ->
                    PendingImageThumb(
                        img = state.pendingImages[idx],
                        index = idx + 1,
                        onTap = { viewModel.appendImageTag(idx + 1) },
                        onRemove = { viewModel.removeChatImage(idx) }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconActionButton(
                icon = Icons.Default.AddPhotoAlternate,
                contentDescription = "添加图片",
                onClick = onPick
            )
            Spacer(Modifier.width(4.dp))
            GptTextField(
                value = state.draft,
                onValueChange = viewModel::updateChatDraft,
                label = "",
                placeholder = "输入消息……",
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 4
            )
            Spacer(Modifier.width(4.dp))
            // 支持连续提交：按钮始终可用，有进行中任务时显示计数徽标
            val pendingCount = state.messages.count { it.isLoading }
            IconButton(onClick = viewModel::sendChat) {
                BadgedBox(
                    badge = {
                        if (pendingCount > 0) {
                            Badge(
                                containerColor = GptColors.ChampagneGold,
                                contentColor = GptColors.Obsidian
                            ) {
                                Text(
                                    pendingCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                ) {
                    if (pendingCount > 0) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = GptColors.ChampagneGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "发送", tint = GptColors.ChampagneGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingImageThumb(
    img: ChatImage,
    index: Int,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    val bmp = remember(img.base64) { ImageCodec.decodeBitmap(img.base64, maxSide = 256) }
    Box(Modifier.size(68.dp).clickable(onClick = onTap)) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        // 序号徽标（左下），用户一看就知道点击后会插入 "image N"
        Surface(
            color = GptColors.ChampagneGold,
            contentColor = GptColors.Obsidian,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(3.dp)
        ) {
            Text(
                "image $index",
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = GptColors.WarmWhite)
        }
    }
}



@Composable
private fun SizePresetRow(selected: ImageSize, onSelect: (ImageSize) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(ImageSize.values(), key = { it.name }) { size ->
            Surface(
                onClick = { onSelect(size) },
                color = if (selected.name == size.name) GptColors.ChampagneGold else GptColors.Charcoal,
                contentColor = if (selected.name == size.name) GptColors.Obsidian else GptColors.Muted,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (selected.name == size.name) GptColors.ChampagneGold else GptColors.Steel)
            ) {
                Text(
                    size.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

