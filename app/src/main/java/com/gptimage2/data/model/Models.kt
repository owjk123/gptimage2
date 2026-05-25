package com.gptimage2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** APIYI 提供的两个 HTTP 端口（16888 协议）。 */
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

/**
 * 根据 APIYI 官方文档的标准分辨率预设
 * 来源: https://help.apiyi.com/why-choose-apiyi-gpt-image-2-official-api.html
 */
enum class ImageSize(val label: String, val value: String) {
    AUTO("自动", "auto"),
    SQUARE_1024("1024×1024 (1:1)", "1024x1024"),
    PORTRAIT_1024_1536("1024×1536 (2:3 竖版)", "1024x1536"),
    LANDSCAPE_1536_1024("1536×1024 (3:2 横版)", "1536x1024"),
    PORTRAIT_1024_1792("1024×1792 (9:16 竖屏)", "1024x1792"),
    LANDSCAPE_1792_1024("1792×1024 (16:9 宽屏)", "1792x1024"),
    SQUARE_2048("2048×2048 (2K 方形)", "2048x2048"),
    LANDSCAPE_2048_1152("2048×1152 (16:9)", "2048x1152"),
    LANDSCAPE_2560_1440("2560×1440 (2K+ 实验性)", "2560x1440"),
    LANDSCAPE_3840_2160("3840×2160 (4K 宽屏)", "3840x2160"),
    PORTRAIT_2160_3840("2160×3840 (4K 竖屏)", "2160x3840")
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
