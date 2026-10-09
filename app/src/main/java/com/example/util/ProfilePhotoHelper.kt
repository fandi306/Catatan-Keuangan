package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ProfilePhotoHelper {

    val PRESET_AVATARS = listOf(
        PresetAvatar("avatar_male", "👨‍💼", "Profesional"),
        PresetAvatar("avatar_female", "👩‍💼", "Eksekutif"),
        PresetAvatar("avatar_dev", "🧑‍💻", "Developer"),
        PresetAvatar("avatar_investor", "💰", "Investor"),
        PresetAvatar("avatar_target", "🎯", "Perencana"),
        PresetAvatar("avatar_rocket", "🚀", "Visioner")
    )

    data class PresetAvatar(val id: String, val emoji: String, val label: String)

    /**
     * Reads a chosen image URI, downsizes it to max 240x240, handles orientation,
     * compresses to JPEG (~10-15KB), and returns a base64 data URL string ("data:image/jpeg;base64,...")
     * which syncs instantly to Firestore across all devices.
     */
    fun uriToBase64DataUrl(context: Context, uri: Uri): String? {
        return try {
            // First decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // Determine sample size
            val maxDim = 240
            var inSampleSize = 1
            while (origWidth / (inSampleSize * 2) >= maxDim && origHeight / (inSampleSize * 2) >= maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Check EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (_: Exception) {}

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            // Downscale to exactly <= maxDim
            val curWidth = sampledBitmap.width
            val curHeight = sampledBitmap.height
            val scale = (maxDim.toFloat() / maxOf(curWidth, curHeight)).coerceAtMost(1f)
            matrix.postScale(scale, scale)

            val finalBitmap = Bitmap.createBitmap(
                sampledBitmap, 0, 0, curWidth, curHeight, matrix, true
            )

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val bytes = outputStream.toByteArray()
            outputStream.close()

            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves an image model for Coil AsyncImage or returns null for emoji/empty.
     * Decodes Base64 data URLs to Bitmap so they render reliably on any device without network calls.
     */
    fun resolveImageModel(photoUri: String?): Any? {
        if (photoUri.isNullOrBlank()) return null
        if (photoUri.startsWith("avatar_") || photoUri.startsWith("emoji:")) return null // Rendered via emoji/preset UI
        if (photoUri.startsWith("data:image/") && photoUri.contains(";base64,")) {
            val base64Part = photoUri.substringAfter(";base64,")
            return try {
                val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }
        return photoUri
    }

    /**
     * Checks if the photoUri represents a preset avatar or custom emoji.
     */
    fun getPresetEmoji(photoUri: String?): String? {
        if (photoUri.isNullOrBlank()) return null
        if (photoUri.startsWith("emoji:")) {
            return photoUri.removePrefix("emoji:")
        }
        return PRESET_AVATARS.find { it.id == photoUri }?.emoji
    }
}
