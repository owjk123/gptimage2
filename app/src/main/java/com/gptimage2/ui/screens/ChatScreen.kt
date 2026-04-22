package com.gptimage2.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gptimage2.data.model.ChatImage
import com.gptimage2.data.model.ChatMessage
import com.gptimage2.data.model.ChatRole
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GptTextField
import com.gptimage2.ui.components.IconActionButton
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.util.ImageCodec
import com.gptimage2.viewmodel.ChatState
import com.gptimage2.viewmodel.MainViewModel

@Composable
fun ChatScreen(state: ChatState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val (bytes, mime) = ImageCodec.readUri(context, uri) ?: return@rememberLauncherForActivityResult
            viewModel.addChatImage(ChatImage(base64 = ImageCodec.makePreview(bytes, 1024), mimeType = mime))
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize()) {
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
                    items(state.messages) { msg -> MessageBubble(msg) }
                }
            }
        }

        ChatComposer(state = state, viewModel = viewModel, onPick = { picker.launch("image/*") })
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
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
                        items(msg.images) { img ->
                            val bytes = ImageCodec.base64ToBytes(img.base64)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
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
                    Text("错误: ${msg.errorMessage}", color = GptColors.Error, style = MaterialTheme.typography.bodyMedium)
                } else if (msg.text.isNotBlank()) {
                    Text(msg.text, color = GptColors.WarmWhite, style = MaterialTheme.typography.bodyMedium)
                }
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
        if (state.pendingImages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(state.pendingImages.size) { idx ->
                    val img = state.pendingImages[idx]
                    Box(Modifier.size(64.dp)) {
                        val bytes = ImageCodec.base64ToBytes(img.base64)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
                        IconButton(
                            onClick = { viewModel.removeChatImage(idx) },
                            modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = GptColors.WarmWhite)
                        }
                    }
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
                minLines = 1
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = viewModel::sendChat, enabled = !state.isSending) {
                if (state.isSending) {
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
