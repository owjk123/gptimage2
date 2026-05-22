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

/** Image size with grouping for UI display. */
enum class SizeGroup(val label: String) {
    SQUARE("方形"),
    PORTRAIT("竖版"),
    LANDSCAPE("横版"),
    ULTRA_WIDE("超宽"),
    AUTO("自动")
}

data class ImageSize(
    val label: String,
    val value: String,
    val group: SizeGroup
) {
    companion object {
        val AUTO = ImageSize("自动", "auto", SizeGroup.AUTO)
        val SQUARE_1024 = ImageSize("1024×1024", "1024x1024", SizeGroup.SQUARE)
        val SQUARE_2048 = ImageSize("2048×2048", "2048x2048", SizeGroup.SQUARE)
        val PORTRAIT_1024_1536 = ImageSize("1024×1536", "1024x1536", SizeGroup.PORTRAIT)
        val PORTRAIT_1536_2048 = ImageSize("1536×2048", "1536x2048", SizeGroup.PORTRAIT)
        val LANDSCAPE_1536_1024 = ImageSize("1536×1024", "1536x1024", SizeGroup.LANDSCAPE)
        val LANDSCAPE_2048_1152 = ImageSize("2048×1152", "2048x1152", SizeGroup.LANDSCAPE)
        val LANDSCAPE_2048_1536 = ImageSize("2048×1536", "2048x1536", SizeGroup.LANDSCAPE)
        val ULTRA_WIDE_2048_896 = ImageSize("2048×896", "2048x896", SizeGroup.ULTRA_WIDE)
        val ULTRA_WIDE_2304_1024 = ImageSize("2304×1024", "2304x1024", SizeGroup.ULTRA_WIDE)

        val ALL = listOf(
            AUTO,
            SQUARE_1024, SQUARE_2048,
            PORTRAIT_1024_1536, PORTRAIT_1536_2048,
            LANDSCAPE_1536_1024, LANDSCAPE_2048_1152, LANDSCAPE_2048_1536,
            ULTRA_WIDE_2048_896, ULTRA_WIDE_2304_1024
        )

        fun byValue(value: String): ImageSize = ALL.find { it.value == value } ?: AUTO
    }
}

enum class OutputCount(val label: String, val value: Int) {
    ONE("1 张", 1),
    TWO("2 张", 2),
    FOUR("4 张", 4)
}

enum class Quality(val label: String, val value: String) {
    LOW("低", "low"),
    MEDIUM("中", "medium"),
    HIGH("高", "high"),
    AUTO("自动", "auto");

    companion object {
        val ALL = listOf(AUTO, LOW, MEDIUM, HIGH)
    }
}

enum class OutputFormat(val label: String, val value: String) {
    PNG("PNG", "png"),
    JPEG("JPEG", "jpeg"),
    WEBP("WebP", "webp");

    companion object {
        val ALL = listOf(PNG, JPEG, WEBP)
    }
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

/** Prompt template for quick generation */
data class PromptTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val prefix: String,
    val isBuiltIn: Boolean = true
)

val BUILT_IN_TEMPLATES = listOf(
    PromptTemplate("portrait", "人像摄影", "Ultra realistic portrait photography of", true),
    PromptTemplate("landscape", "风景", "Breathtaking landscape photography of", true),
    PromptTemplate("product", "产品渲染", "Professional product photography of", true),
    PromptTemplate("illustration", "插画", "Digital illustration of", true),
    PromptTemplate("anime", "动漫风格", "Anime style illustration of", true)
)
