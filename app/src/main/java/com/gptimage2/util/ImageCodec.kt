package com.gptimage2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageCodec {

    /** 读取 Uri 的原始字节 + MIME。 */
    fun readUri(context: Context, uri: Uri): Pair<ByteArray, String>? = runCatching {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@runCatching null
        bytes to mime
    }.getOrNull()

    fun bytesToBase64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun base64ToBytes(data: String): ByteArray =
        Base64.decode(data, Base64.DEFAULT)

    /**
     * 内存安全地生成缩略图 base64（JPEG Q80）：
     * 1) inJustDecodeBounds 先读尺寸；
     * 2) 计算 inSampleSize 把最长边降到 ≤ 2*maxSide；
     * 3) 解码后再精确缩放到 maxSide。
     * 保证不会因为巨图直接 OOM 崩溃。
     */
    fun makePreview(bytes: ByteArray, maxSide: Int = 512): String = runCatching {
        val (w, h) = probeBounds(bytes)
        if (w <= 0 || h <= 0) return@runCatching bytesToBase64(bytes)

        val sample = computeInSampleSize(w, h, maxSide * 2)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: return@runCatching bytesToBase64(bytes)

        val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height)
        val bmp = if (scale >= 1f) decoded else Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        if (bmp !== decoded) decoded.recycle()
        bytesToBase64(out.toByteArray())
    }.getOrElse { bytesToBase64(bytes) }

    /** 从 base64 安全解码成 Bitmap，限制最大边以避免 OOM。 */
    fun decodeBitmap(base64: String, maxSide: Int = 1024): Bitmap? = runCatching {
        val bytes = base64ToBytes(base64)
        val (w, h) = probeBounds(bytes)
        if (w <= 0 || h <= 0) return@runCatching null
        val sample = computeInSampleSize(w, h, maxSide)
        BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }.getOrNull()

    /** 从文件路径安全解码成 Bitmap。 */
    fun decodeFile(path: String, maxSide: Int = 1024): Bitmap? = runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOpts)
        val sample = computeInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxSide)
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }.getOrNull()

    private fun probeBounds(bytes: ByteArray): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return opts.outWidth to opts.outHeight
    }

    private fun computeInSampleSize(srcW: Int, srcH: Int, maxSide: Int): Int {
        if (srcW <= 0 || srcH <= 0 || maxSide <= 0) return 1
        var sample = 1
        val longest = maxOf(srcW, srcH)
        while (longest / sample > maxSide) sample *= 2
        return sample
    }
}
