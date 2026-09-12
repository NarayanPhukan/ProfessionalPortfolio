package com.narayan.portfolioadmin.data.util

import android.util.Base64

object ImageUtils {
    /**
     * Converts a URL or base64 data URI string to a model suitable for Coil.
     * If the string is a data URI (e.g. data:image/jpeg;base64,...), it decodes
     * it into a ByteArray so Coil's ByteArrayFetcher can render it directly.
     */
    fun parseImageModel(url: String?): Any? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return if (trimmed.startsWith("data:", ignoreCase = true) && trimmed.contains("base64,", ignoreCase = true)) {
            try {
                val base64Data = trimmed.substringAfter("base64,")
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                trimmed
            }
        } else {
            trimmed
        }
    }
}
