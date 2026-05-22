package com.gptimage2.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.gptimage2.data.local.GptImageDatabase
import com.gptimage2.data.model.*
import com.gptimage2.repository.ChatRepository
import com.gptimage2.repository.GalleryRepository
import com.gptimage2.repository.ImageEditRepository
import com.gptimage2.repository.ImageGenRepository
import com.gptimage2.util.ApiKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class AppTab { TEXT_TO_IMAGE, IMAGE_EDIT, CHAT, GALLERY, SETTINGS }

data class T2IState(
    val prompt: String = "",
    val size: ImageSize = ImageSize.SQUARE_1024,
    val count: OutputCount = OutputCount.ONE,
    val quality: Quality = Quality.AUTO,
    val outputFormat: OutputFormat = OutputFormat.PNG,
    val outputCompression: Int = 90,
    val selectedTemplate: PromptTemplate? = null,
    val isGenerating: Boolean = false,
    val latestResults: List<String> = emptyList()
)

data class EditState(
    val prompt: String = "",
    val size: ImageSize = ImageSize.AUTO,
    val count: OutputCount = OutputCount.ONE,
    val quality: Quality = Quality.AUTO,
    val outputFormat: OutputFormat = OutputFormat.PNG,
    val outputCompression: Int = 90,
    val inputFidelity: Float = 0.5f,
    val references: List<EditReference> = emptyList(),
    val isGenerating: Boolean = false,
    val latestResults: List<String> = emptyList()
)

data class ChatAspectPreset(val label: String, val prefix: String)

val CHAT_ASPECT_PRESETS = listOf(
    ChatAspectPreset("方形", "1024×1024 方图 / 1:1 方形构图。"),
    ChatAspectPreset("横版", "横版 16:9 / 宽屏 16:9 电影画幅。"),
    ChatAspectPreset("竖版", "竖版 9:16 / 手机海报 9:16。"),
    ChatAspectPreset("超宽", "横幅 21:9 超宽银幕。"),
    ChatAspectPreset("经典", "4:3 标准画幅 / 3:2 经典画幅。")
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val pendingImages: List<ChatImage> = emptyList(),
    val aspectPreset: ChatAspectPreset? = null,
    val isSending: Boolean = false
)

data class SettingsState(
    val apiKey: String = "",
    val baseUrl: String = ApiKeyManager.DEFAULT_BASE_URL,
    val model: String = ApiKeyManager.DEFAULT_MODEL
)

enum class GalleryFilter(val label: String) {
    ALL("全部"),
    T2I("文生图"),
    EDIT("图像编辑"),
    CHAT("对话")
}

data class GalleryState(
    val filter: GalleryFilter = GalleryFilter.ALL
)

data class MainUiState(
    val tab: AppTab = AppTab.CHAT,
    val t2i: T2IState = T2IState(),
    val edit: EditState = EditState(),
    val chat: ChatState = ChatState(),
    val settings: SettingsState = SettingsState(),
    val gallery: List<GalleryImage> = emptyList(),
    val galleryState: GalleryState = GalleryState(),
    val toastMessage: String? = null
)

class MainViewModel(
    private val appContext: Context,
    private val imageGen: ImageGenRepository,
    private val imageEdit: ImageEditRepository,
    private val chatRepo: ChatRepository,
    private val galleryRepo: GalleryRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MainUiState(
        settings = SettingsState(
            apiKey = ApiKeyManager.loadApiKey(appContext),
            baseUrl = ApiKeyManager.loadBaseUrl(appContext),
            model = ApiKeyManager.loadModel(appContext)
        )
    ))
    val uiState: StateFlow<MainUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            galleryRepo.getAllImages().collect { list ->
                _ui.update { it.copy(gallery = list) }
            }
        }
    }

    fun setTab(tab: AppTab) = _ui.update { it.copy(tab = tab) }
    fun dismissToast() = _ui.update { it.copy(toastMessage = null) }
    fun showToastPublic(msg: String) = showToast(msg)
    private fun showToast(msg: String) = _ui.update { it.copy(toastMessage = msg) }

    // ── Text-to-Image ───────────────────────────────────────
    fun updateT2IPrompt(v: String) = _ui.update { it.copy(t2i = it.t2i.copy(prompt = v)) }
    fun selectT2ISize(v: ImageSize) = _ui.update { it.copy(t2i = it.t2i.copy(size = v)) }
    fun selectT2ICount(v: OutputCount) = _ui.update { it.copy(t2i = it.t2i.copy(count = v)) }
    fun selectT2IQuality(v: Quality) = _ui.update { it.copy(t2i = it.t2i.copy(quality = v)) }
    fun selectT2IOutputFormat(v: OutputFormat) = _ui.update { it.copy(t2i = it.t2i.copy(outputFormat = v)) }
    fun updateT2ICompression(v: Int) = _ui.update { it.copy(t2i = it.t2i.copy(outputCompression = v)) }
    fun selectT2ITemplate(v: PromptTemplate?) {
        _ui.update { it.copy(t2i = it.t2i.copy(selectedTemplate = v)) }
        v?.let { t ->
            val currentPrompt = _ui.value.t2i.prompt
            val prefix = if (currentPrompt.isBlank()) t.prefix else "${t.prefix} $currentPrompt"
            updateT2IPrompt(prefix)
        }
    }

    fun runTextToImage() {
        val st = _ui.value.t2i
        if (st.prompt.isBlank()) { showToast("请输入提示词"); return }
        _ui.update { it.copy(t2i = it.t2i.copy(isGenerating = true, latestResults = emptyList())) }
        viewModelScope.launch {
            val isOfficialModel = ApiKeyManager.loadModel(appContext).contains("gpt-image-2")
            val result = imageGen.generate(
                prompt = st.prompt,
                size = st.size.value,
                count = st.count.value,
                quality = st.quality.value,
                outputFormat = if (isOfficialModel) st.outputFormat.value else null,
                outputCompression = if (isOfficialModel && st.outputFormat != OutputFormat.PNG) st.outputCompression else null
            )
            result.onSuccess { imgs ->
                imgs.forEach { b64 ->
                    galleryRepo.saveGenerated(
                        id = UUID.randomUUID().toString(),
                        base64Data = b64,
                        prompt = st.prompt,
                        size = st.size.label,
                        endpoint = EndpointKind.TEXT_TO_IMAGE.name
                    )
                }
                _ui.update { it.copy(t2i = it.t2i.copy(isGenerating = false, latestResults = imgs)) }
                showToast("生成完成")
            }.onFailure { e ->
                _ui.update { it.copy(t2i = it.t2i.copy(isGenerating = false)) }
                showToast("生成失败: ${e.message}")
            }
        }
    }

    // ── Image Edit ───────────────────────────────────────────
    fun updateEditPrompt(v: String) = _ui.update { it.copy(edit = it.edit.copy(prompt = v)) }
    fun selectEditSize(v: ImageSize) = _ui.update { it.copy(edit = it.edit.copy(size = v)) }
    fun selectEditCount(v: OutputCount) = _ui.update { it.copy(edit = it.edit.copy(count = v)) }
    fun selectEditQuality(v: Quality) = _ui.update { it.copy(edit = it.edit.copy(quality = v)) }
    fun selectEditOutputFormat(v: OutputFormat) = _ui.update { it.copy(edit = it.edit.copy(outputFormat = v)) }
    fun updateEditCompression(v: Int) = _ui.update { it.copy(edit = it.edit.copy(outputCompression = v)) }
    fun updateEditInputFidelity(v: Float) = _ui.update { it.copy(edit = it.edit.copy(inputFidelity = v)) }

    fun addEditReference(ref: EditReference) {
        val current = _ui.value.edit.references
        if (current.size >= 4) { showToast("最多 4 张参考图"); return }
        _ui.update { it.copy(edit = it.edit.copy(references = current + ref)) }
    }

    fun removeEditReference(id: String) =
        _ui.update { it.copy(edit = it.edit.copy(references = it.edit.references.filter { r -> r.id != id })) }

    fun runImageEdit() {
        val st = _ui.value.edit
        if (st.prompt.isBlank()) { showToast("请输入提示词"); return }
        if (st.references.isEmpty()) { showToast("请至少添加一张参考图"); return }
        _ui.update { it.copy(edit = it.edit.copy(isGenerating = true, latestResults = emptyList())) }
        viewModelScope.launch {
            val isOfficialModel = ApiKeyManager.loadModel(appContext).contains("gpt-image-2")
            val result = imageEdit.edit(
                prompt = st.prompt,
                size = st.size.value,
                count = st.count.value,
                references = st.references,
                quality = st.quality.value,
                outputFormat = if (isOfficialModel) st.outputFormat.value else null,
                outputCompression = if (isOfficialModel && st.outputFormat != OutputFormat.PNG) st.outputCompression else null,
                inputFidelity = if (isOfficialModel) st.inputFidelity else null
            )
            result.onSuccess { imgs ->
                imgs.forEach { b64 ->
                    galleryRepo.saveGenerated(
                        id = UUID.randomUUID().toString(),
                        base64Data = b64,
                        prompt = st.prompt,
                        size = st.size.label,
                        endpoint = EndpointKind.IMAGE_EDIT.name
                    )
                }
                _ui.update { it.copy(edit = it.edit.copy(isGenerating = false, latestResults = imgs)) }
                showToast("编辑完成")
            }.onFailure { e ->
                _ui.update { it.copy(edit = it.edit.copy(isGenerating = false)) }
                showToast("编辑失败: ${e.message}")
            }
        }
    }

    // ── Chat ─────────────────────────────────────────────────
    fun updateChatDraft(v: String) = _ui.update { it.copy(chat = it.chat.copy(draft = v)) }

    fun addChatImage(img: ChatImage) {
        val current = _ui.value.chat.pendingImages
        if (current.size >= 4) { showToast("最多 4 张图片"); return }
        _ui.update { it.copy(chat = it.chat.copy(pendingImages = current + img)) }
    }

    fun removeChatImage(index: Int) {
        val list = _ui.value.chat.pendingImages.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _ui.update { it.copy(chat = it.chat.copy(pendingImages = list)) }
        }
    }

    fun toggleAspectPreset(preset: ChatAspectPreset) = _ui.update {
        val next = if (it.chat.aspectPreset?.label == preset.label) null else preset
        it.copy(chat = it.chat.copy(aspectPreset = next))
    }

    /** Append "image N" (1-based) to the draft, preserving existing text. */
    fun appendImageTag(index: Int) = _ui.update {
        val draft = it.chat.draft
        val sep = if (draft.isEmpty() || draft.endsWith(" ") || draft.endsWith("\n")) "" else " "
        it.copy(chat = it.chat.copy(draft = draft + sep + "image $index"))
    }

    fun resetChat() = _ui.update { it.copy(chat = ChatState()) }

    fun sendChat() {
        val chat = _ui.value.chat
        if (chat.draft.isBlank() && chat.pendingImages.isEmpty()) return
        val prefix = chat.aspectPreset?.let { "${it.prefix} " } ?: ""
        val userMsg = ChatMessage(
            role = ChatRole.USER,
            text = prefix + chat.draft,
            images = chat.pendingImages
        )
        val pending = ChatMessage(role = ChatRole.ASSISTANT, isLoading = true)
        _ui.update {
            it.copy(chat = it.chat.copy(
                messages = it.chat.messages + userMsg + pending,
                draft = "",
                pendingImages = emptyList(),
                isSending = true
            ))
        }
        viewModelScope.launch {
            val context = _ui.value.chat.messages.filter { m -> !m.isLoading }
            val result = chatRepo.send(context)
            _ui.update { state ->
                val newList = state.chat.messages.toMutableList()
                val idx = newList.indexOfFirst { it.id == pending.id }
                if (idx >= 0) {
                    newList[idx] = result.getOrElse {
                        pending.copy(isLoading = false, errorMessage = it.message)
                    }
                }
                state.copy(chat = state.chat.copy(messages = newList, isSending = false))
            }
            result.onSuccess { reply ->
                reply.images.forEach { img ->
                    galleryRepo.saveGenerated(
                        id = UUID.randomUUID().toString(),
                        base64Data = img.base64,
                        prompt = userMsg.text.ifBlank { "(chat)" },
                        size = "-",
                        endpoint = EndpointKind.CHAT.name
                    )
                }
            }
        }
    }

    // ── Gallery ─────────────────────────────────────────────
    fun setGalleryFilter(filter: GalleryFilter) =
        _ui.update { it.copy(galleryState = it.galleryState.copy(filter = filter)) }

    fun getFilteredGallery(): List<GalleryImage> {
        val filter = _ui.value.galleryState.filter
        val all = _ui.value.gallery
        return when (filter) {
            GalleryFilter.ALL -> all
            GalleryFilter.T2I -> all.filter { it.endpoint == EndpointKind.TEXT_TO_IMAGE.name }
            GalleryFilter.EDIT -> all.filter { it.endpoint == EndpointKind.IMAGE_EDIT.name }
            GalleryFilter.CHAT -> all.filter { it.endpoint == EndpointKind.CHAT.name }
        }
    }

    fun shareGalleryImage(image: GalleryImage) {
        viewModelScope.launch {
            val file = File(image.imagePath)
            if (!file.exists()) {
                showToast("图片文件不存在")
                return@launch
            }
            try {
                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(Intent.createChooser(shareIntent, "分享图片").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                showToast("分享失败: ${e.message}")
            }
        }
    }

    fun saveToAlbum(image: GalleryImage) {
        viewModelScope.launch {
            val r = galleryRepo.saveToAlbum(image.id, image.imagePath)
            r.onSuccess { showToast("已保存到相册") }
                .onFailure { showToast("保存失败: ${it.message}") }
        }
    }

    fun deleteGalleryImage(image: GalleryImage) {
        viewModelScope.launch {
            galleryRepo.deleteImage(image)
            showToast("已删除")
        }
    }

    // ── Settings ────────────────────────────────────────────
    fun updateApiKeyDraft(v: String) = _ui.update { it.copy(settings = it.settings.copy(apiKey = v)) }
    fun updateBaseUrlDraft(v: String) = _ui.update { it.copy(settings = it.settings.copy(baseUrl = v)) }
    fun updateModelDraft(v: String) = _ui.update { it.copy(settings = it.settings.copy(model = v)) }

    fun saveSettings() {
        val s = _ui.value.settings
        ApiKeyManager.saveApiKey(appContext, s.apiKey)
        ApiKeyManager.saveBaseUrl(appContext, s.baseUrl.ifBlank { ApiKeyManager.DEFAULT_BASE_URL })
        ApiKeyManager.saveModel(appContext, s.model.ifBlank { ApiKeyManager.DEFAULT_MODEL })
        showToast("设置已保存")
    }

    fun testConnection() {
        viewModelScope.launch {
            showToast("正在测试连接...")
            val result = imageGen.testConnection()
            result.onSuccess {
                showToast("连接成功 ✓")
            }.onFailure { e ->
                showToast("连接失败: ${e.message}")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            ApiKeyManager.init(appContext)
            val db = Room.databaseBuilder(
                appContext,
                GptImageDatabase::class.java,
                GptImageDatabase.DATABASE_NAME
            ).build()
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                appContext = appContext,
                imageGen = ImageGenRepository(appContext),
                imageEdit = ImageEditRepository(appContext),
                chatRepo = ChatRepository(appContext),
                galleryRepo = GalleryRepository(appContext, db.galleryDao())
            ) as T
        }
    }
}
