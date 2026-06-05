package com.gptimage2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** APIYI 支持的模型列表。 */
const val MODEL_ALL = "gpt-image-2-all"
const val MODEL_VIP = "gpt-image-2-vip"
const val MODEL_OFFICIAL = "gpt-image-2"

val SUPPORTED_MODELS = listOf(MODEL_ALL, MODEL_VIP, MODEL_OFFICIAL)

/** APIYI 提供的 HTTP 端口。 */
data class ApiEndpoint(val label: String, val url: String)

val API_ENDPOINTS = listOf(
    ApiEndpoint("美国优化", "https://vip.apiyi.com"),
    ApiEndpoint("CF-CDN", "https://api-cf.apiyi.com"),
    ApiEndpoint("大陆优化1", "https://api.apiyi.com"),
    ApiEndpoint("大陆优化2", "https://b.apiyi.com")
)

/** Which endpoint produced an image. */
enum class EndpointKind(val label: String, val short: String) {
    TEXT_TO_IMAGE("文生图", "T2I"),
    IMAGE_EDIT("图像编辑", "EDIT"),
    CHAT("多模态对话", "CHAT")
}

/**
 * 通用尺寸预设（适用于 gpt-image-2-all / gpt-image-2）
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

/**
 * gpt-image-2-vip 专属：3 档分辨率
 */
enum class VipTier(val label: String) {
    FAST_1K("1K Fast — 草稿"),
    RECOMMENDED_2K("2K Recommended — 终稿"),
    DETAIL_4K("4K Detail — 印刷/大屏")
}

/**
 * gpt-image-2-vip 30 档固定尺寸（10 比例 × 3 档）
 */
data class VipSize(val label: String, val value: String, val tier: VipTier)

val VIP_SIZES: List<VipSize> = listOf(
    // 1K Fast
    VipSize("1:1 方图", "1280x1280", VipTier.FAST_1K),
    VipSize("2:3 竖版", "848x1280", VipTier.FAST_1K),
    VipSize("3:2 横版", "1280x848", VipTier.FAST_1K),
    VipSize("3:4 竖版", "960x1280", VipTier.FAST_1K),
    VipSize("4:3 标准", "1280x960", VipTier.FAST_1K),
    VipSize("4:5 社交", "1024x1280", VipTier.FAST_1K),
    VipSize("5:4 大方", "1280x1024", VipTier.FAST_1K),
    VipSize("9:16 故事", "720x1280", VipTier.FAST_1K),
    VipSize("16:9 宽屏", "1280x720", VipTier.FAST_1K),
    VipSize("21:9 影院", "1280x544", VipTier.FAST_1K),
    // 2K Recommended
    VipSize("1:1 方图", "2048x2048", VipTier.RECOMMENDED_2K),
    VipSize("2:3 竖版", "1360x2048", VipTier.RECOMMENDED_2K),
    VipSize("3:2 横版", "2048x1360", VipTier.RECOMMENDED_2K),
    VipSize("3:4 竖版", "1536x2048", VipTier.RECOMMENDED_2K),
    VipSize("4:3 标准", "2048x1536", VipTier.RECOMMENDED_2K),
    VipSize("4:5 社交", "1632x2048", VipTier.RECOMMENDED_2K),
    VipSize("5:4 大方", "2048x1632", VipTier.RECOMMENDED_2K),
    VipSize("9:16 故事", "1152x2048", VipTier.RECOMMENDED_2K),
    VipSize("16:9 宽屏", "2048x1152", VipTier.RECOMMENDED_2K),
    VipSize("21:9 影院", "2048x864", VipTier.RECOMMENDED_2K),
    // 4K Detail
    VipSize("1:1 方图", "2880x2880", VipTier.DETAIL_4K),
    VipSize("2:3 竖版", "2336x3520", VipTier.DETAIL_4K),
    VipSize("3:2 横版", "3520x2336", VipTier.DETAIL_4K),
    VipSize("3:4 竖版", "2480x3312", VipTier.DETAIL_4K),
    VipSize("4:3 标准", "3312x2480", VipTier.DETAIL_4K),
    VipSize("4:5 社交", "2560x3216", VipTier.DETAIL_4K),
    VipSize("5:4 大方", "3216x2560", VipTier.DETAIL_4K),
    VipSize("9:16 故事", "2160x3840", VipTier.DETAIL_4K),
    VipSize("16:9 宽屏", "3840x2160", VipTier.DETAIL_4K),
    VipSize("21:9 影院", "3840x1632", VipTier.DETAIL_4K),
)

/** 按分辨率档位筛选 VIP 尺寸 */
fun vipSizesForTier(tier: VipTier): List<VipSize> = VIP_SIZES.filter { it.tier == tier }

/** 判断模型是否为 VIP（不支持 quality/n） */
fun isVipModel(model: String): Boolean = model.contains("vip", ignoreCase = true)

enum class OutputCount(val label: String, val value: Int) {
    ONE("1 张", 1),
    TWO("2 张", 2),
    FOUR("4 张", 4)
}

data class EditReference(
    val id: String = UUID.randomUUID().toString(),
    val bytes: ByteArray,
    val mimeType: String,
    val previewBase64: String,
    val uploadOriginal: Boolean = false
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
