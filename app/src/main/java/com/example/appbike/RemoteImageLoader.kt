package com.example.appbike

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal object RemoteImageLoader {
    private const val MAX_REMOTE_IMAGE_BYTES = 20L * 1024L * 1024L
    private val byteCache = object : LruCache<String, ByteArray>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    fun loadBitmap(source: String, maxDimension: Int = 1280): Bitmap? {
        if (source.isBlank()) return null
        val bytes = byteCache.get(source) ?: loadBytes(source)?.also {
            byteCache.put(source, it)
        } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxDimension * 2 ||
            bounds.outHeight / sample > maxDimension * 2
        ) {
            sample *= 2
        }
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        )
    }

    fun loadLocalBitmap(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxDimension * 2 ||
            bounds.outHeight / sample > maxDimension * 2
        ) {
            sample *= 2
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
            )
        } ?: return null

        val orientation = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return decoded
        return Bitmap.createBitmap(
            decoded,
            0,
            0,
            decoded.width,
            decoded.height,
            matrix,
            true
        ).also { rotated ->
            if (rotated !== decoded) decoded.recycle()
        }
    }

    private fun loadBytes(source: String): ByteArray? = when {
        source.startsWith("appbike-market-photo://") -> {
            val parts = source.removePrefix("appbike-market-photo://").split('/', limit = 2)
            if (parts.size != 2) null else RemoteConnections.loadMarketplacePhoto(parts[0], parts[1])
        }
        source.startsWith("appbike-junta-photo://") -> {
            val parts = source.removePrefix("appbike-junta-photo://").split('/', limit = 2)
            if (parts.size != 2) null else RemoteConnections.loadMeetupPhoto(parts[0], parts[1])
        }
        source.startsWith("appbike-photo://") ->
            RemoteConnections.loadBikePhoto(source.removePrefix("appbike-photo://"))
        source.startsWith("https://") || source.startsWith("http://") -> {
            val connection = URL(source).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("Accept", "image/*")
                if (connection.responseCode !in 200..299 ||
                    connection.contentLengthLong > MAX_REMOTE_IMAGE_BYTES
                ) {
                    null
                } else connection.inputStream.use {
                    it.readBytesWithLimit(MAX_REMOTE_IMAGE_BYTES)
                }
            } finally {
                connection.disconnect()
            }
        }
        else -> null
    }

    private fun InputStream.readBytesWithLimit(maxBytes: Long): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) return null
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
