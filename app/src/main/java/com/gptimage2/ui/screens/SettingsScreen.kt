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
import com.gptimage2.data.model.MODEL_ALL
import com.gptimage2.data.model.MODEL_VIP
import com.gptimage2.data.model.MODEL_OFFICIAL
import com.gptimage2.ui.components.ChipSelector
import com.gptimage2.ui.components.GoldButton
import com.gptimage2.ui.components.GptCard
import com.gptimage2.ui.components.GptTextField
import com.gptimage2.ui.components.SectionLabel
import com.gptimage2.ui.theme.GptColors
import com.gptimage2.viewmodel.MainViewModel
import com.gptimage2.viewmodel.SettingsState

private data class ModelOption(val id: String, val label: String, val description: String)

private val MODEL_OPTIONS = listOf(
    ModelOption(
        id = MODEL_ALL,
        label = "gpt-image-2-all",
        description = "APIYI 反代版，$0.03/次，约30-60s出图。不支持 size 参数（尺寸写进prompt）。"
    ),
    ModelOption(
        id = MODEL_VIP,
        label = "gpt-image-2-vip ⭐",
        description = "VIP 逆向 Codex 线路，$0.03/次，约90-150s出图。支持 30 档固定尺寸含4K，不支持 quality/n。"
    ),
    ModelOption(
        id = MODEL_OFFICIAL,
        label = "gpt-image-2",
        description = "OpenAI 官方代理，按 token 计费，支持 quality/4K上采样/input_fidelity。"
    )
)

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
            SectionLabel("模型")
            Spacer(Modifier.height(8.dp))
            val currentModel = MODEL_OPTIONS.firstOrNull { it.id == state.model }
                ?: ModelOption(state.model, state.model, "自定义模型 ID")
            val items = if (currentModel in MODEL_OPTIONS) MODEL_OPTIONS
            else MODEL_OPTIONS + currentModel
            ChipSelector(
                items = items,
                selected = currentModel,
                labelOf = { it.label },
                onSelect = { viewModel.updateModelDraft(it.id) }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                currentModel.description,
                color = GptColors.Muted,
                style = MaterialTheme.typography.bodySmall
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
            SectionLabel("支持的端点")
            Spacer(Modifier.height(6.dp))
            Text("• POST /v1/chat/completions（多模态对话，本 App 主入口）", color = GptColors.Muted, style = MaterialTheme.typography.bodySmall)
            Text("• POST /v1/images/generations（文生图）", color = GptColors.Muted, style = MaterialTheme.typography.bodySmall)
            Text("• POST /v1/images/edits（多图编辑）", color = GptColors.Muted, style = MaterialTheme.typography.bodySmall)
        }

        GptCard {
            SectionLabel("提示")
            Spacer(Modifier.height(6.dp))
            Text(
                "• gpt-image-2-all: 最快，约30-60s，尺寸写进prompt\n" +
                "• gpt-image-2-vip: 支持30档固定尺寸含4K，约90-150s，不支持 quality/n\n" +
                "• gpt-image-2: 官方代理按 token 计费，需要 quality/input_fidelity 时使用\n" +
                "• API Key 仅存储在本机 SharedPreferences，不会上传。\n" +
                "• 若返回「连接被中断」，切换 API 端口通常可恢复。",
                color = GptColors.Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
