package com.gptimage2.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gptimage2.data.model.isVipModel
import com.gptimage2.util.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * POST {baseUrl}/v1/images/generations
 */
class ImageGenRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val apiKey get() = ApiKeyManager.loadApiKey(context)
    private val model get() = ApiKeyManager.loadModel(context)
    private val endpoint get() = "${ApiKeyManager.loadBaseUrl(context)}/v1/images/generations"

    suspend fun generate(
        prompt: String,
        size: String,
        count: Int,
        quality: String = "auto"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("请先在设置中填写 API Key"))

        val isVip = isVipModel(model)
        val needsParallel = isVip || model == MODEL_OFFICIAL || count <= 1

        if (needsParallel && count > 1) {
            val deferreds = (1..count).map { idx ->
                async {
                    Log.d("ImageGenRepo", "Starting parallel request $idx/$count")
                    generateSingle(prompt, size, quality, isVip)
                }
            }
            val results = deferreds.awaitAll()

            val images = mutableListOf<String>()
            val errors = mutableListOf<String>()
            results.forEachIndexed { idx, r ->
                r.onSuccess { images.addAll(it) }
                    .onFailure { errors.add("请求 ${idx+1}: ${it.message}") }
            }

            when {
                images.isEmpty() && errors.isNotEmpty() -> Result.failure(Exception("全部失败: ${errors.first()}"))
                else -> Result.success(images)
            }
        } else {
            generateSingle(prompt, size, quality, isVip, if (isVip) 1 else count)
        }
    }

    private suspend fun generateSingle(
        prompt: String,
        size: String,
        quality: String,
        isVip: Boolean,
        count: Int = 1
    ): Result<List<String>> {
        var lastError: Exception? = null
        val maxAttempts = 3
        val retryDelayMs = 15_000L

        for (attempt in 1..maxAttempts) {
            val result = withContext(Dispatchers.IO) {
                try {
                    val body = JsonObject().apply {
                        addProperty("model", model)
                        addProperty("prompt", prompt)
                        if (size != "auto") addProperty("size", size)
                        if (!isVip) {
                            if (quality != "auto") addProperty("quality", quality)
                            addProperty("n", count)
                        }
                        addProperty("response_format", "b64_json")
                    }

                    val response = client.newCall(
                        Request.Builder()
                            .url(endpoint)
                            .header("Authorization", "Bearer $apiKey")
                            .header("Content-Type", "application/json")
                            .post(gson.toJson(body).toRequestBody(JSON_MEDIA))
                            .build()
                    ).execute()

                    val raw = response.body?.string()
                    val code = response.code

                    if (code in 500..599 && attempt < maxAttempts && code != 501) {
                        Log.w("ImageGenRepo", "Attempt $attempt failed with $code, retrying in ${retryDelayMs}ms...")
                        return@withContext Result.failure(RetryableException(code, raw))
                    }

                    if (!response.isSuccessful || raw == null) {
                        return@withContext Result.failure(Exception(errorFor(code, raw)))
                    }
                    Result.success(parseImages(raw))
                } catch (e: Exception) {
                    if (e is RetryableException) return@withContext Result.failure(e)
                    Log.e("ImageGenRepo", "generateSingle failed", e)
                    Result.failure(e)
                }
            }

            result.onSuccess { return it }
            result.onFailure { e ->
                if (e is RetryableException) {
                    lastError = e
                    delay(retryDelayMs)
                    continue
                }
                return result
            }
        }
        return Result.failure(lastError ?: Exception("请求失败（已重试 $maxAttempts 次）"))
    }

    companion object {
        const val MODEL_ALL = "gpt-image-2-all"
        const val MODEL_VIP = "gpt-image-2-vip"
        const val MODEL_OFFICIAL = "gpt-image-2"
        val JSON_MEDIA = "application/json".toMediaType()
    }
}

private class RetryableException(val code: Int, rawBody: String?) : Exception(
    "上游返回 $code${rawBody?.take(100)?.let { ": $it" } ?: ""}"
)

fun errorFor(code: Int, raw: String?): String = when (code) {
    400 -> "请求参数错误: ${raw?.take(200) ?: "-"}"
    401 -> "API Key 无效，请在设置中重新填写"
    402 -> "余额不足，请到 apiyi.com 充值"
    403 -> "没有该模型的访问权限"
    404 -> "接口地址错误（检查 Base URL）"
    429 -> "请求过于频繁，请稍后再试"
    500, 502, 503, 504 -> "服务器繁忙，请稍后再试"
    else -> "请求失败 ($code): ${raw?.take(200) ?: "-"}"
}

fun parseImages(raw: String): List<String> {
    val root = JsonParser.parseString(raw).asJsonObject
    val data = root.getAsJsonArray("data")
        ?: throw Exception(root.getAsJsonObject("error")?.get("message")?.asString ?: "空响应")
    if (data.size() == 0) throw Exception("响应 data 为空")
    val out = mutableListOf<String>()
    for (el in data) {
        val o = el.asJsonObject
        when {
            o.has("b64_json") -> {
                val b64 = o.get("b64_json").asString
                out += if (b64.startsWith("data:")) b64.substringAfter(",") else b64
            }
            o.has("url") -> out += downloadAsBase64(o.get("url").asString)
            else -> throw Exception("无法识别的图片字段")
        }
    }
    return out
}

private fun downloadAsBase64(url: String): String {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    val response = client.newCall(Request.Builder().url(url).build()).execute()
    val bytes = response.body?.bytes() ?: throw Exception("下载图片失败")
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}
