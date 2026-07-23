package com.example.appbike

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL

internal object RemoteImageLoader {
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
                if (connection.responseCode !in 200..299) null else connection.inputStream.use {
                    it.readBytes()
                }
            } finally {
                connection.disconnect()
            }
        }
        else -> null
    }
}
