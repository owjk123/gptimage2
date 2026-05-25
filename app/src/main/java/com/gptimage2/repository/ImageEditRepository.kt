package com.gptimage2.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gptimage2.data.model.EditReference
import com.gptimage2.util.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Image edit: routes to the correct endpoint based on model.
 *
 * - gpt-image-2 (official) → POST /v1/images/edits (multipart/form-data)
 * - gpt-image-2-all (reverse-proxy) → POST /v1/chat/completions (JSON, image_url)
 */
class ImageEditRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val apiKey get() = ApiKeyManager.loadApiKey(context)
    private val model get() = ApiKeyManager.loadModel(context)
    private val baseUrl get() = ApiKeyManager.loadBaseUrl(context)

    /** Whether the current model uses the chat/completions path for image editing. */
    private fun usesChatPath(): Boolean =
        model.contains("2-all", ignoreCase = true) || model.contains("gpt-image-2-all", ignoreCase = true)

    /**
     * Edit images with parallel support.
     * Official gpt-image-2 only supports n=1 per request, so we spawn parallel requests.
     */
    suspend fun edit(
        prompt: String,
        size: String,
        count: Int,
        references: List<EditReference>,
        quality: String = "auto"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("请先在设置中填写 API Key"))
        if (references.isEmpty()) return@withContext Result.failure(Exception("至少需要一张参考图"))

        // For official gpt-image-2, use parallel requests (only supports n=1)
        val needsParallel = model == "gpt-image-2" || count <= 1

        if (needsParallel && count > 1) {
            // Parallel: launch count requests, each with 1 image
            val deferreds = (1..count).map { idx ->
                async {
                    Log.d("ImageEditRepo", "Starting parallel edit request $idx/$count")
                    if (usesChatPath()) {
                        editViaChatSingle(prompt, size, references, quality)
                    } else {
                        editViaEditsEndpointSingle(prompt, size, references, quality)
                    }
                }
            }
            val results = deferreds.awaitAll()

            // Collect all successful images
            val images = mutableListOf<String>()
            val errors = mutableListOf<String>()
            results.forEachIndexed { idx, r ->
                r.onSuccess { images.addAll(it) }
                    .onFailure { errors.add("请求 ${idx+1}: ${it.message}") }
            }

            if (images.isEmpty() && errors.isNotEmpty()) {
                Result.failure(Exception("全部失败: ${errors.first()}"))
            } else if (images.size < count) {
                Log.w("ImageEditRepo", "Partial success: ${images.size}/$count")
                Result.success(images)
            } else {
                Result.success(images)
            }
        } else {
            // Single request with n=count
            if (usesChatPath()) {
                editViaChatSingle(prompt, size, references, quality, count)
            } else {
                editViaEditsEndpointSingle(prompt, size, references, quality, count)
            }
        }
    }

    /**
     * Official gpt-image-2 path: single request to /v1/images/edits
     */
    private suspend fun editViaEditsEndpointSingle(
        prompt: String,
        size: String,
        references: List<EditReference>,
        quality: String,
        count: Int = 1
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("response_format", "b64_json")

            // n parameter for proxy models (official only supports n=1)
            if (model != "gpt-image-2") builder.addFormDataPart("n", count.toString())
            if (size != "auto") builder.addFormDataPart("size", size)
            if (quality != "auto") builder.addFormDataPart("quality", quality)

            references.forEachIndexed { idx, ref ->
                val ext = if (ref.mimeType.endsWith("png")) "png" else "jpg"
                builder.addFormDataPart(
                    "image",
                    "ref_${idx + 1}_${UUID.randomUUID()}.$ext",
                    ref.bytes.toRequestBody(ref.mimeType.toMediaType())
                )
            }

            val response = client.newCall(
                Request.Builder()
                    .url("$baseUrl/v1/images/edits")
                    .header("Authorization", "Bearer $apiKey")
                    .post(builder.build())
                    .build()
            ).execute()

            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                return@withContext Result.failure(Exception(editErrorFor(response.code, raw)))
            }
            Result.success(parseImages(raw))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * gpt-image-2-all (reverse-proxy) path: single request to /v1/chat/completions
     */
    private suspend fun editViaChatSingle(
        prompt: String,
        size: String,
        references: List<EditReference>,
        quality: String,
        count: Int = 1
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Build the user message with text + image_url content blocks
            val contentArr = JsonArray()

            // Add reference images first
            references.forEachIndexed { idx, ref ->
                val b64 = Base64.encodeToString(ref.bytes, Base64.NO_WRAP)
                contentArr.add(JsonObject().apply {
                    addProperty("type", "image_url")
                    add("image_url", JsonObject().apply {
                        addProperty("url", "data:${ref.mimeType};base64,$b64")
                    })
                })
            }

            // Build enhanced prompt with size/quality hints
            val enhancedPrompt = buildString {
                append(prompt)
                if (size != "auto") append("\nOutput size: $size")
                if (quality != "auto") append(", quality: $quality")
                if (count > 1) append("\nGenerate $count images")
            }

            contentArr.add(JsonObject().apply {
                addProperty("type", "text")
                addProperty("text", enhancedPrompt)
            })

            val body = JsonObject().apply {
                addProperty("model", model)
                addProperty("stream", false)
                add("messages", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        add("content", contentArr)
                    })
                })
            }

            val response = client.newCall(
                Request.Builder()
                    .url("$baseUrl/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()

            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                return@withContext Result.failure(Exception(editErrorFor(response.code, raw)))
            }
            Result.success(parseChatImages(raw))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Parse images/edits response: { data: [ {b64_json | url} ] } */
    private fun parseImages(raw: String): List<String> {
        val root = JsonParser.parseString(raw).asJsonObject
        val data = root.getAsJsonArray("data")
            ?: throw Exception(root.getAsJsonObject("error")?.get("message")?.asString ?: "空响应")
        if (data.size() == 0) throw Exception("响应 data 为空")
        val out = mutableListOf<String>()
        for (el in data) {
            val o = el.asJsonObject
            when {
                o.has("b64_json") -> out += o.get("b64_json").asString
                o.has("url") -> out += downloadAsBase64(o.get("url").asString)
                else -> throw Exception("无法识别的图片字段")
            }
        }
        return out
    }

    /** Parse chat/completions response: extract base64 images from markdown or data URLs in content. */
    private fun parseChatImages(raw: String): List<String> {
        val root = JsonParser.parseString(raw).asJsonObject
        val choice = root.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
            ?: throw Exception(root.getAsJsonObject("error")?.get("message")?.asString ?: "空响应")
        val content = choice.getAsJsonObject("message").get("content").asString

        val images = mutableListOf<String>()

        // Match ![alt](data:image/...;base64,...) markdown images
        val mdDataRegex = Regex("""!\[[^\]]*\]\((data:image/[a-zA-Z0-9+\-.]+;base64,([A-Za-z0-9+/=]+))\)""")
        mdDataRegex.findAll(content).forEach { m ->
            images += m.groupValues[2]
        }

        // Match bare data:image/...;base64,... URLs
        val dataRegex = Regex("""data:image/[a-zA-Z0-9+\-.]+;base64,([A-Za-z0-9+/=]+)""")
        var textPart = mdDataRegex.replace(content, "")
        dataRegex.findAll(textPart).forEach { m ->
            if (m.groupValues[1] !in images) images += m.groupValues[1]
        }
        textPart = dataRegex.replace(textPart, "")

        // Match ![alt](https://...png/jpg/webp)
        val mdUrlRegex = Regex("""!\[[^\]]*\]\((https?://[^)\s]+\.(?:png|jpe?g|webp))\)""", RegexOption.IGNORE_CASE)
        mdUrlRegex.findAll(content).forEach { m ->
            val b64 = runCatching { downloadAsBase64(m.groupValues[1]) }.getOrNull()
            if (b64 != null) images += b64
        }

        // Match bare https://...png/jpg/webp
        val urlRegex = Regex("""https?://[^\s)\]]+\.(?:png|jpe?g|webp)""", RegexOption.IGNORE_CASE)
        urlRegex.findAll(textPart).forEach { m ->
            val b64 = runCatching { downloadAsBase64(m.value) }.getOrNull()
            if (b64 != null && b64 !in images) images += b64
        }

        if (images.isEmpty()) {
            throw Exception("模型未返回图片，请尝试更明确的编辑指令")
        }
        return images
    }

    private fun downloadAsBase64(url: String): String {
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        val bytes = resp.body?.bytes() ?: throw Exception("下载图片失败")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

private fun editErrorFor(code: Int, raw: String?): String = when (code) {
    400 -> "请求参数错误: ${raw?.take(200) ?: "-"}"
    401 -> "API Key 无效，请在设置中重新填写"
    402 -> "余额不足，请到 apiyi.com 充值"
    403 -> "没有该模型的访问权限"
    404 -> "接口地址错误（检查 Base URL）"
    429 -> "请求过于频繁，请稍后再试"
    500, 502, 503, 504 -> "服务器繁忙，请稍后再试"
    else -> "请求失败 ($code): ${raw?.take(200) ?: "-"}"
}
