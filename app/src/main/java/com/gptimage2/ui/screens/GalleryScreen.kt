package com.gptimage2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gptimage2.data.model.GalleryImage
import com.gptimage2.ui.components.ChipSelector
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.OutlineGoldButton
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.util.ImageCodec
import com.gptimage2.viewmodel.GalleryFilter
import com.gptimage2.viewmodel.MainViewModel

private val endpointGradient = Brush.verticalGradient(
    colors = listOf(
        GptColors.Obsidian.copy(alpha = 0.8f),
        GptColors.Obsidian.copy(alpha = 0f)
    )
)

@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    filter: GalleryFilter,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<GalleryImage?>(null) }
    
    val filteredImages = remember(images, filter) {
        when (filter) {
            GalleryFilter.ALL -> images
            GalleryFilter.T2I -> images.filter { it.endpoint == "TEXT_TO_IMAGE" }
            GalleryFilter.EDIT -> images.filter { it.endpoint == "IMAGE_EDIT" }
            GalleryFilter.CHAT -> images.filter { it.endpoint == "CHAT" }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GalleryFilter.entries.forEach { f ->
                val isSelected = f == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) GptColors.ChampagneGold else GptColors.Charcoal
                        )
                        .clickable { viewModel.setGalleryFilter(f) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = f.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) GptColors.Obsidian else GptColors.Muted
                    )
                }
            }
        }

        if (filteredImages.isEmpty()) {
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
            items(filteredImages, key = { it.id }) { img ->
                GalleryThumb(img, onClick = { selected = img })
            }
        }
    }

    selected?.let { img ->
        val fullBmp = remember(img.id) { ImageCodec.decodeFile(img.imagePath, maxSide = 2048) }
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = {
                GoldButton(
                    text = "保存到相册",
                    onClick = {
                        viewModel.saveToAlbum(img)
                        selected = null
                    },
                    leadingIcon = Icons.Default.Download
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineGoldButton(
                        text = "分享",
                        onClick = {
                            viewModel.shareGalleryImage(img)
                        },
                        leadingIcon = Icons.Default.Share
                    )
                    OutlineGoldButton(
                        text = "删除",
                        onClick = {
                            viewModel.deleteGalleryImage(img)
                            selected = null
                        },
                        leadingIcon = Icons.Default.Delete
                    )
                    OutlineGoldButton(
                        text = "关闭",
                        onClick = { selected = null },
                        leadingIcon = Icons.Default.Close
                    )
                }
            },
            containerColor = GptColors.Onyx,
            titleContentColor = GptColors.ChampagneGold,
            textContentColor = GptColors.WarmWhite,
            title = { Text(img.endpoint, color = GptColors.ChampagneGold) },
            text = {
                Column {
                    if (fullBmp != null) {
                        Image(
                            bitmap = fullBmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        img.prompt,
                        color = GptColors.WarmWhite,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
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
        // Gradient overlay at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.TopCenter)
                .background(endpointGradient)
        )
        // Endpoint label at top left
        Text(
            image.endpoint.take(4),
            color = GptColors.WarmWhite,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        )
        // Prompt preview at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GptColors.Obsidian.copy(alpha = 0f),
                            GptColors.Obsidian.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(6.dp)
        ) {
            Text(
                image.prompt,
                color = GptColors.WarmWhite,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
