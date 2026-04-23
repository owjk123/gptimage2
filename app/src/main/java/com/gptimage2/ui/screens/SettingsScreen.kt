package com.gptimage2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gptimage2.data.model.API_ENDPOINTS
import com.gptimage2.data.model.ApiEndpoint
import com.gptimage2.ui.components.ChipSelector
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.GptCard
import com.gptimage2.ui.components.GptTextField
import com.gptimage2.ui.components.SectionLabel
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.viewmodel.MainViewModel
import com.gptimage2.viewmodel.SettingsState

@Composable
fun SettingsScreen(state: SettingsState, viewModel: MainViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GptCard {
            SectionLabel("API 接口（HTTP 端口 16888）")
            Spacer(Modifier.height(8.dp))
            Text(
                "选一个响应最快的端口，或在下方自定义。",
                color = GptColors.Muted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            val currentPreset = API_ENDPOINTS.firstOrNull { it.url == state.baseUrl.trimEnd('/') }
                ?: ApiEndpoint("自定义", state.baseUrl)
            val options = if (currentPreset in API_ENDPOINTS) API_ENDPOINTS
            else API_ENDPOINTS + currentPreset
            ChipSelector(
                items = options,
                selected = currentPreset,
                labelOf = { it.label },
                onSelect = { viewModel.updateBaseUrlDraft(it.url) }
            )
            Spacer(Modifier.height(12.dp))
            GptTextField(
                value = state.baseUrl,
                onValueChange = viewModel::updateBaseUrlDraft,
                label = "Base URL",
                placeholder = "https://api.apiyi.com",
                singleLine = true
            )
        }

        GptCard {
            SectionLabel("API 凭证")
            Spacer(Modifier.height(10.dp))
            GptTextField(
                value = state.apiKey,
                onValueChange = viewModel::updateApiKeyDraft,
                label = "API Key",
                placeholder = "sk-...",
                singleLine = true
            )
        }

        GoldButton(
            text = "保存配置",
            onClick = viewModel::saveSettings,
            leadingIcon = Icons.Default.Save,
            modifier = Modifier.fillMaxWidth()
        )

        GptCard {
            SectionLabel("模型")
            Spacer(Modifier.height(6.dp))
            Text("gpt-image-2-all", color = GptColors.WarmWhite, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SectionLabel("支持的端点")
            Spacer(Modifier.height(6.dp))
            Text("• /v1/images/generations", color = GptColors.Muted, style = MaterialTheme.typography.bodySmall)
            Text("• /v1/images/edits", color = GptColors.Muted, style = MaterialTheme.typography.bodySmall)
            Text("• /v1/chat/completions", color = GptColors.Muted, style = MaterialTheme.typography.bodySmall)
        }

        GptCard {
            SectionLabel("提示")
            Spacer(Modifier.height(6.dp))
            Text(
                "API Key 仅存储在本机 SharedPreferences，不会上传。到 api.apiyi.com 注册并创建 key 后填入即可。若对话生图返回 \"连接被中断\"，切换到其他端口通常可恢复。",
                color = GptColors.Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
