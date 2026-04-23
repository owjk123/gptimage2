package com.gptimage2.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.gptimage2.data.local.GptImageDatabase
import com.gptimage2.data.model.ChatImage
import com.gptimage2.data.model.ChatMessage
import com.gptimage2.data.model.ChatRole
import com.gptimage2.data.model.EditReference
import com.gptimage2.data.model.EndpointKind
import com.gptimage2.data.model.GalleryImage
import com.gptimage2.data.model.ImageSize
import com.gptimage2.data.model.OutputCount
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
import java.util.UUID

enum class AppTab { TEXT_TO_IMAGE, IMAGE_EDIT, CHAT, GALLERY, SETTINGS }

data class T2IState(
    val prompt: String = "",
    val size: ImageSize = ImageSize.SQUARE_1024,
    val count: OutputCount = OutputCount.ONE,
    val isGenerating: Boolean = false,
    val latestResults: List<String> = emptyList()
)

data class EditState(
    val prompt: String = "",
    val size: ImageSize = ImageSize.AUTO,
    val count: OutputCount = OutputCount.ONE,
    val references: List<EditReference> = emptyList(),
    val isGenerating: Boolean = false,
    val latestResults: List<String> = emptyList()
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val pendingImages: List<ChatImage> = emptyList(),
    val isSending: Boolean = false
)

data class SettingsState(
    val apiKey: String = "",
    val baseUrl: String = ApiKeyManager.DEFAULT_BASE_URL
)

data class MainUiState(
    val tab: AppTab = AppTab.TEXT_TO_IMAGE,
    val t2i: T2IState = T2IState(),
    val edit: EditState = EditState(),
    val chat: ChatState = ChatState(),
    val settings: SettingsState = SettingsState(),
    val gallery: List<GalleryImage> = emptyList(),
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
            baseUrl = ApiKeyManager.loadBaseUrl(appContext)
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

    fun runTextToImage() {
        val st = _ui.value.t2i
        if (st.prompt.isBlank()) { showToast("请输入提示词"); return }
        _ui.update { it.copy(t2i = it.t2i.copy(isGenerating = true, latestResults = emptyList())) }
        viewModelScope.launch {
            val result = imageGen.generate(st.prompt, st.size.value, st.count.value)
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
            val result = imageEdit.edit(st.prompt, st.size.value, st.count.value, st.references)
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

    fun resetChat() = _ui.update { it.copy(chat = ChatState()) }

    fun sendChat() {
        val chat = _ui.value.chat
        if (chat.draft.isBlank() && chat.pendingImages.isEmpty()) return
        val userMsg = ChatMessage(
            role = ChatRole.USER,
            text = chat.draft,
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

    fun saveSettings() {
        val s = _ui.value.settings
        ApiKeyManager.saveApiKey(appContext, s.apiKey)
        ApiKeyManager.saveBaseUrl(appContext, s.baseUrl.ifBlank { ApiKeyManager.DEFAULT_BASE_URL })
        showToast("设置已保存")
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
