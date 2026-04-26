package com.gptimage2.repository

import android.content.Context
import android.util.Log
import com.gptimage2.data.model.EditReference
import com.gptimage2.util.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * POST {baseUrl}/v1/images/edits
 * multipart/form-data: model, prompt, image (repeated), size?, n?, response_format
 * Prompt references: "image 1", "image 2", ...
 */
class ImageEditRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val apiKey get() = ApiKeyManager.loadApiKey(context)
    private val model get() = ApiKeyManager.loadModel(context)
    private val endpoint get() = "${ApiKeyManager.loadBaseUrl(context)}/v1/images/edits"

    suspend fun edit(
        prompt: String,
        size: String,
        count: Int,
        references: List<EditReference>,
        quality: String = "auto",
        inputFidelity: String? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("请先在设置中填写 API Key"))
        if (references.isEmpty()) return@withContext Result.failure(Exception("至少需要一张参考图"))

        try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("response_format", "b64_json")
                .addFormDataPart("n", count.toString())
            if (size != "auto") builder.addFormDataPart("size", size)
            if (quality != "auto") builder.addFormDataPart("quality", quality)
            if (inputFidelity != null) builder.addFormDataPart("input_fidelity", inputFidelity)

            references.forEachIndexed { _, ref ->
                val ext = if (ref.mimeType.endsWith("png")) "png" else "jpg"
                builder.addFormDataPart(
                    "image",
                    "ref_${UUID.randomUUID()}.$ext",
                    ref.bytes.toRequestBody(ref.mimeType.toMediaType())
                )
            }

            val response = client.newCall(
                Request.Builder()
                    .url(endpoint)
                    .header("Authorization", "Bearer $apiKey")
                    .post(builder.build())
                    .build()
            ).execute()

            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                return@withContext Result.failure(Exception(errorFor(response.code, raw)))
            }
            Result.success(parseImages(raw))
        } catch (e: Exception) {
            Log.e("ImageEditRepo", "edit failed", e)
            Result.failure(e)
        }
    }
}
