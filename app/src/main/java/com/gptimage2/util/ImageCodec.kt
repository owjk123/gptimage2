package com.gptimage2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageCodec {

    fun readUri(context: Context, uri: Uri): Pair<ByteArray, String>? {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        return bytes to mime
    }

    fun bytesToBase64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun base64ToBytes(data: String): ByteArray =
        Base64.decode(data, Base64.DEFAULT)

    /** Downscale for preview thumbnails used inside Compose LazyRow, preserves aspect ratio. */
    fun makePreview(bytes: ByteArray, maxSide: Int = 512): String {
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return bytesToBase64(bytes)
        val scale = maxSide.toFloat() / maxOf(raw.width, raw.height)
        val bmp = if (scale >= 1f) raw else Bitmap.createScaledBitmap(
            raw,
            (raw.width * scale).toInt().coerceAtLeast(1),
            (raw.height * scale).toInt().coerceAtLeast(1),
            true
        )
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return bytesToBase64(out.toByteArray())
    }
}
