package com.gptimage2.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gptimage2.data.model.ChatImage
import com.gptimage2.data.model.ChatMessage
import com.gptimage2.data.model.ChatRole
import com.gptimage2.data.model.ImageSize
import com.gptimage2.util.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.TimeUnit

/**
 * POST {baseUrl}/v1/chat/completions (multimodal)
 * 多模态生图对话：包含图片的请求可能耗时 60-180s，服务端偶发闪断。
 */
class ChatRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .callTimeout(600, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(4, 30, TimeUnit.SECONDS))
        .build()

    private val gson = Gson()

    private val apiKey get() = ApiKeyManager.loadApiKey(context)
    private val endpoint get() = "${ApiKeyManager.loadBaseUrl(context)}/v1/chat/completions"

    suspend fun send(history: List<ChatMessage>, size: ImageSize = ImageSize.SQUARE_1024): Result<ChatMessage> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("请先在设置中填写 API Key"))
        val model = ApiKeyManager.loadModel(context)
        val bodyJson = gson.toJson(JsonObject().apply {
            addProperty("model", model)
            addProperty("stream", false)
            if (size != "auto") addProperty("size", size.value)
            add("messages", buildMessages(history))
        })

        var lastError: Throwable? = null
        for (attempt in 1..3) {
            try {
                val response = client.newCall(
                    Request.Builder()
                        .url(endpoint)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .header("Connection", "close")
                        .post(bodyJson.toRequestBody("application/json".toMediaType()))
                        .build()
                ).execute()

                val raw = response.body?.string()
                if (!response.isSuccessful || raw == null) {
                    return@withContext Result.failure(Exception(errorFor(response.code, raw)))
                }
                return@withContext Result.success(parseReply(raw))
            } catch (e: SocketException) {
                lastError = e
                Log.w("ChatRepo", "attempt $attempt aborted: ${e.message}")
                delay(1500L * attempt)
            } catch (e: IOException) {
                lastError = e
                Log.w("ChatRepo", "attempt $attempt IO: ${e.message}")
                if (attempt == 3) break
                delay(1500L * attempt)
            } catch (e: Exception) {
                Log.e("ChatRepo", "send failed", e)
                return@withContext Result.failure(e)
            }
        }
        Result.failure(Exception("连接被中断，请检查网络或在设置页切换到其他端口后重试 (${lastError?.message ?: "-"})"))
    }

    private fun buildMessages(history: List<ChatMessage>): JsonArray {
        val arr = JsonArray()
        var lastRole: String? = null
        for (msg in history) {
            if (msg.isLoading || msg.errorMessage != null) continue
            val isUser = msg.role == ChatRole.USER
            val role = if (isUser) "user" else "assistant"

            // 防御：如果跳过 error assistant 导致连续两条 user，插入空 assistant 占位
            if (role == "user" && lastRole == "user") {
                arr.add(JsonObject().apply {
                    addProperty("role", "assistant")
                    addProperty("content", "[请求失败，已重试]")
                })
            }

            val obj = JsonObject()
            obj.addProperty("role", role)
            if (!isUser) {
                // assistant 消息只传文本，绝不能带 image_url
                // OpenAI chat/completions API 不支持 assistant 角色 image_url，
                // 且 base64 图片会让请求体爆炸导致 API 忽略历史或报错
                val text = msg.text.ifBlank { "[已生成图片]" }
                obj.addProperty("content", text)
            } else if (msg.images.isEmpty()) {
                obj.addProperty("content", msg.text)
            } else {
                // user 消息可以带图片（多模态）
                val content = JsonArray()
                if (msg.text.isNotBlank()) {
                    content.add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", msg.text)
                    })
                }
                for (img in msg.images) {
                    content.add(JsonObject().apply {
                        addProperty("type", "image_url")
                        add("image_url", JsonObject().apply {
                            addProperty("url", "data:${img.mimeType};base64,${img.base64}")
                        })
                    })
                }
                obj.add("content", content)
            }
            arr.add(obj)
            lastRole = role
        }
        return arr
    }

    private fun parseReply(raw: String): ChatMessage {
        val root = JsonParser.parseString(raw).asJsonObject
        val choice = root.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
            ?: throw Exception(root.getAsJsonObject("error")?.get("message")?.asString ?: "空响应")
        val content = choice.getAsJsonObject("message").get("content").asString

        val images = mutableListOf<ChatImage>()
        var textPart = content

        val dataRegex = Regex("""data:(image/[a-zA-Z0-9+\-.]+);base64,([A-Za-z0-9+/=]+)""")
                    addProperty("role", "assistant")
                    addProperty("content", "[请求失败，已重试]")
                })
            }

            val obj = JsonObject()
            obj.addProperty("role", role)
            if (!isUser) {
                // assistant 消息只传文本，绝不能带 image_url
                // OpenAI chat/completions API 不支持 assistant 角色 image_url，
                // 且 base64 图片会让请求体爆炸导致 API 忽略历史或报错
                val text = msg.text.ifBlank { "[已生成图片]" }
                obj.addProperty("content", text)
            } else if (msg.images.isEmpty()) {
                obj.addProperty("content", msg.text)
            } else {
                // user 消息可以带图片（多模态）
                val content = JsonArray()
                if (msg.text.isNotBlank()) {
                    content.add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", msg.text)
                    })
                }
                for (img in msg.images) {
                    content.add(JsonObject().apply {
                        addProperty("type", "image_url")
                        add("image_url", JsonObject().apply {
                            addProperty("url", "data:${img.mimeType};base64,${img.base64}")
                        })
                    })
                }
                obj.add("content", content)
            }
            arr.add(obj)
            lastRole = role
        }
        return arr
    }

    private fun parseReply(raw: String): ChatMessage {
        val root = JsonParser.parseString(raw).asJsonObject
        val choice = root.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
            ?: throw Exception(root.getAsJsonObject("error")?.get("message")?.asString ?: "空响应")
        val content = choice.getAsJsonObject("message").get("content").asString

        val images = mutableListOf<ChatImage>()
        var textPart = content

        val dataRegex = Regex("""data:(image/[a-zA-Z0-9+\-.]+);base64,([A-Za-z0-9+/=]+)""")

        // 先把 ![alt](data:image/...) 整个 markdown 外壳一起吞掉，避免残留 ![image]([image])
        val mdDataRegex = Regex("""!\[[^\]]*\]\((data:image/[a-zA-Z0-9+\-.]+;base64,[A-Za-z0-9+/=]+)\)""")
        mdDataRegex.findAll(content).forEach { m ->
            val inner = m.groupValues[1]
            val d = dataRegex.find(inner) ?: return@forEach
            images += ChatImage(base64 = d.groupValues[2], mimeType = d.groupValues[1])
        }
        textPart = mdDataRegex.replace(textPart, "")

        // 再抓任何裸露的 data URL（没被 markdown 包的）
        dataRegex.findAll(textPart).forEach { m ->
            images += ChatImage(base64 = m.groupValues[2], mimeType = m.groupValues[1])
        }
        textPart = dataRegex.replace(textPart, "")

        // 然后是 markdown 包裹的 http(s) 图片
        val mdUrlRegex = Regex("""!\[[^\]]*\]\((https?://[^)\s]+\.(?:png|jpe?g|webp))\)""", RegexOption.IGNORE_CASE)
        mdUrlRegex.findAll(content).forEach { m ->
            appendRemoteImage(images, m.groupValues[1])
        }
        textPart = mdUrlRegex.replace(textPart, "")

        // 最后是裸露的 http(s) 图片地址
        val urlRegex = Regex("""https?://[^\s)\]]+\.(?:png|jpe?g|webp)""", RegexOption.IGNORE_CASE)
        urlRegex.findAll(textPart).forEach { m -> appendRemoteImage(images, m.value) }
        textPart = urlRegex.replace(textPart, "")

        return ChatMessage(
            role = ChatRole.ASSISTANT,
            text = textPart.trim(),
            images = images
        )
    }

    private fun appendRemoteImage(images: MutableList<ChatImage>, url: String) {
        runCatching {
            val bytes = downloadBytes(url)
            val mime = when {
                url.endsWith(".png", true) -> "image/png"
                url.endsWith(".webp", true) -> "image/webp"
                else -> "image/jpeg"
            }
            images += ChatImage(
                base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP),
                mimeType = mime
            )
        }
    }

    private fun downloadBytes(url: String): ByteArray {
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        return resp.body?.bytes() ?: throw Exception("下载图片失败")
    }
}

