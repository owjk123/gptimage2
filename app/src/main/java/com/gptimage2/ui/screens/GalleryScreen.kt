package com.gptimage2.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.gptimage2.data.model.GalleryImage
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.OutlineGoldButton
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.util.ImageCodec
import com.gptimage2.viewmodel.MainViewModel

@Composable
fun GalleryScreen(images: List<GalleryImage>, viewModel: MainViewModel) {
    var selected by remember { mutableStateOf<GalleryImage?>(null) }
    val context = LocalContext.current

    if (images.isEmpty()) {
        EmptyHint("图库为空，先去生成一张吧")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images, key = { it.id }) { img ->
            GalleryThumb(img) { selected = img }
        }
    }
    selected?.let { img ->
        FullScreenImagePreview(
            image = img,
            onDismiss = { selected = null },
            onSaveToAlbum = {
                viewModel.saveToAlbum(img)
                selected = null
            },
            onDelete = {
                viewModel.deleteGalleryImage(img)
                selected = null
            },
            onCopyPrompt = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("GPT Image Prompt", img.prompt))
                viewModel.showToastPublic("提示词已复制")
            }
        )
    }
}

@Composable
private fun FullScreenImagePreview(
    image: GalleryImage,
    onDismiss: () -> Unit,
    onSaveToAlbum: () -> Unit,
    onDelete: () -> Unit,
    onCopyPrompt: () -> Unit
) {
    val fullBmp = remember(image.id) { ImageCodec.decodeFile(image.imagePath, maxSide = 4096) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GptColors.Obsidian
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部工具栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${image.endpoint} | ${image.size}",
                        color = GptColors.ChampagneGold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = GptColors.WarmWhite)
                    }
                }

                // 图片区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (fullBmp != null) {
                        Image(
                            bitmap = fullBmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 底部信息和操作栏
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // 提示词区域（可点击复制）
                    Surface(
                        color = GptColors.Onyx,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable(onClick = onCopyPrompt),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                image.prompt,
                                color = GptColors.WarmWhite,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "复制提示词",
                                tint = GptColors.ChampagneGold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlineGoldButton(
                            text = "删除",
                            onClick = onDelete,
                            leadingIcon = Icons.Default.Delete,
                            modifier = Modifier.weight(1f)
                        )
                        GoldButton(
                            text = "保存到相册",
                            onClick = onSaveToAlbum,
                            leadingIcon = Icons.Default.Download,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryThumb(image: GalleryImage, onClick: () -> Unit) {
    val bmp = remember(image.id) { ImageCodec.decodeFile(image.imagePath, maxSide = 512) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            image.endpoint.take(4),
            color = GptColors.WarmWhite,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        )
    }
}
