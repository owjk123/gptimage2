package com.gptimage2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.viewmodel.AppTab
import com.gptimage2.viewmodel.MainUiState
import com.gptimage2.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(uiState: MainUiState, viewModel: MainViewModel) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissToast()
        }
    }

    Scaffold(
        containerColor = GptColors.Obsidian,
        contentColor = GptColors.WarmWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GPT Image 2",
                        color = GptColors.ChampagneGold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GptColors.Onyx,
                    titleContentColor = GptColors.ChampagneGold
                )
            )
        },
        bottomBar = { BottomBar(uiState.tab, viewModel::setTab) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(GptColors.Obsidian)) {
            when (uiState.tab) {
                AppTab.TEXT_TO_IMAGE -> TextToImageScreen(uiState.t2i, viewModel)
                AppTab.IMAGE_EDIT -> ImageEditScreen(uiState.edit, viewModel)
                AppTab.CHAT -> ChatScreen(uiState.chat, viewModel)
                AppTab.GALLERY -> GalleryScreen(uiState.gallery, viewModel)
                AppTab.SETTINGS -> SettingsScreen(uiState.settings, viewModel)
            }
        }
    }
}

@Composable
private fun BottomBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = GptColors.Onyx, contentColor = GptColors.WarmWhite) {
        entry(current, AppTab.TEXT_TO_IMAGE, "文生图", Icons.Default.AutoAwesome, onSelect)
        entry(current, AppTab.IMAGE_EDIT, "编辑", Icons.Default.Brush, onSelect)
        entry(current, AppTab.CHAT, "对话", Icons.Default.Chat, onSelect)
        entry(current, AppTab.GALLERY, "图库", Icons.Default.PhotoLibrary, onSelect)
        entry(current, AppTab.SETTINGS, "设置", Icons.Default.Settings, onSelect)
    }
}

@Composable
private fun RowScope.entry(
    current: AppTab,
    target: AppTab,
    label: String,
    icon: ImageVector,
    onSelect: (AppTab) -> Unit
) {
    NavigationBarItem(
        selected = current == target,
        onClick = { onSelect(target) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = GptColors.Obsidian,
            selectedTextColor = GptColors.ChampagneGold,
            unselectedIconColor = GptColors.Muted,
            unselectedTextColor = GptColors.Muted,
            indicatorColor = GptColors.ChampagneGold
        )
    )
}
