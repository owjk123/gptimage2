package com.gptimage2.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gptimage2.data.model.GalleryImage
import com.gptimage2.ui.components.EmptyHint
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.IconActionButton
import com.gptimage2.ui.components.OutlineGoldButton
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.viewmodel.MainViewModel

@Composable
fun GalleryScreen(images: List<GalleryImage>, viewModel: MainViewModel) {
    var selected by remember { mutableStateOf<GalleryImage?>(null) }

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
                    val bmp = BitmapFactory.decodeFile(img.imagePath)
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(img.prompt, color = GptColors.WarmWhite, style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }
}

@Composable
private fun GalleryThumb(image: GalleryImage, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        val bmp = BitmapFactory.decodeFile(image.imagePath)
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
