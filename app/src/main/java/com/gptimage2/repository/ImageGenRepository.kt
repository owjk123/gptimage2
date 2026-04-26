package com.gptimage2.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gptimage2.util.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * POST {baseUrl}/v1/images/generations
 * Body (JSON): { model, prompt, size, n, response_format: b64_json }
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
        try {
            val body = JsonObject().apply {
                addProperty("model", model)
                addProperty("prompt", prompt)
                if (size != "auto") addProperty("size", size)
                if (quality != "auto") addProperty("quality", quality)
                addProperty("n", count)
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
            if (!response.isSuccessful || raw == null) {
                return@withContext Result.failure(Exception(errorFor(response.code, raw)))
            }
            Result.success(parseImages(raw))
        } catch (e: Exception) {
            Log.e(TAG, "generate failed", e)
            Result.failure(e)
        }
    }

    companion object {
        /** Default model id; runtime model is read from ApiKeyManager. */
        const val MODEL = "gpt-image-2-all"
        const val MODEL_OFFICIAL = "gpt-image-2"
        val SUPPORTED_MODELS = listOf(MODEL, MODEL_OFFICIAL)
        private const val TAG = "ImageGenRepo"
        val JSON_MEDIA = "application/json".toMediaType()
    }
}

internal fun errorFor(code: Int, raw: String?): String = when (code) {
    401 -> "API Key 无效，请在设置中重新填写"
    402 -> "余额不足，请到 apiyi.com 充值"
    403 -> "没有该模型的访问权限"
    404 -> "接口地址错误（检查 Base URL）"
    429 -> "请求过于频繁，请稍后再试"
    500, 502, 503, 504 -> "服务器繁忙，请稍后再试"
    else -> "请求失败 ($code): ${raw?.take(200) ?: "-"}"
}

/** Parse { data: [ {b64_json | url} ] } into a list of base64 strings.
 *  When the server returns URLs, fetch them once and base64-encode.  */
internal fun parseImages(raw: String): List<String> {
    val root = com.google.gson.JsonParser.parseString(raw).asJsonObject
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

private fun downloadAsBase64(url: String): String {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val response = client.newCall(Request.Builder().url(url).build()).execute()
    val bytes = response.body?.bytes() ?: throw Exception("下载图片失败")
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}
