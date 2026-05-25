package com.gptimage2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** APIYI 提供的四个 HTTP 端口（16888 协议）。 */
data class ApiEndpoint(val label: String, val url: String)

val API_ENDPOINTS = listOf(
    ApiEndpoint("美国优化", "https://vip.apiyi.com"),
    ApiEndpoint("CF-CDN", "https://api-cf.apiyi.com"),
    ApiEndpoint("大陆优化1", "https://api.apiyi.com"),
    ApiEndpoint("大陆优化2", "https://b.apiyi.com")
)

/** Which of the three gpt-image-2-all endpoints produced an image. */
enum class EndpointKind(val label: String, val short: String) {
    TEXT_TO_IMAGE("文生图", "T2I"),
    IMAGE_EDIT("图像编辑", "EDIT"),
    CHAT("多模态对话", "CHAT")
}

enum class ImageSize(val label: String, val value: String) {
    AUTO("自动", "auto"),
    SQUARE_1024("1024×1024", "1024x1024"),
    PORTRAIT_1024_1792("1024×1792 竖", "1024x1792"),
    LANDSCAPE_1792_1024("1792×1024 横", "1792x1024"),
    SQUARE_2048("2048×2048 (2K)", "2048x2048"),
    PORTRAIT_1440_2560("1440×2560 (2K竖)", "1440x2560"),
    LANDSCAPE_2560_1440("2560×1440 (2K横)", "2560x1440"),
    SQUARE_4096("4096×4096 (4K)", "4096x4096"),
    PORTRAIT_2160_3840("2160×3840 (4K竖)", "2160x3840"),
    LANDSCAPE_3840_2160("3840×2160 (4K横)", "3840x2160")
}

enum class OutputCount(val label: String, val value: Int) {
    ONE("1 张", 1),
    TWO("2 张", 2),
    FOUR("4 张", 4)
}

data class EditReference(
    val id: String = UUID.randomUUID().toString(),
    val bytes: ByteArray,
    val mimeType: String,
    val previewBase64: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditReference) return false
        return id == other.id
    }
    override fun hashCode() = id.hashCode()
}

enum class ChatRole { USER, ASSISTANT }

data class ChatImage(
    val base64: String,
    val mimeType: String = "image/png"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String = "",
    val images: List<ChatImage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Entity(tableName = "gallery_images")
data class GalleryImage(
    @PrimaryKey val id: String,
    val prompt: String,
    val imagePath: String,
    val size: String,
    val endpoint: String,
    val createdAt: Long,
    val savedToAlbum: Boolean = false
)
