package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class EncodedUploadFile(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64Payload: String,
    val previewBitmap: Bitmap? = null
)

object ImageCompressionUtil {

    private const val MAX_IMAGE_DIM = 720
    private const val MAX_BASE64_CHARS = 420_000

    fun createCameraCaptureUri(context: Context, prefix: String): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun compressImageFromUri(context: Context, uri: Uri, fallbackName: String): Result<EncodedUploadFile> {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return Result.failure(IllegalArgumentException("Selected file is not a valid image."))
            }

            var inSample = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / inSample > MAX_IMAGE_DIM * 2) {
                inSample *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
                ?: return Result.failure(IllegalArgumentException("Unable to read image stream."))

            val scale = minOf(1f, MAX_IMAGE_DIM.toFloat() / max(decoded.width, decoded.height).toFloat())
            val scaled = if (scale < 1f) {
                val w = max(1, (decoded.width * scale).roundToInt())
                val h = max(1, (decoded.height * scale).roundToInt())
                Bitmap.createScaledBitmap(decoded, w, h, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            var quality = 74
            var jpegBytes: ByteArray
            var encoded: String
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                jpegBytes = out.toByteArray()
                encoded = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                quality -= 10
            } while (encoded.length > MAX_BASE64_CHARS && quality >= 30)

            if (encoded.length > MAX_BASE64_CHARS) {
                return Result.failure(IllegalArgumentException("Image is too large even after compression. Please capture a clearer smaller photo."))
            }

            val fileName = queryFileName(context, uri) ?: "${fallbackName}_${System.currentTimeMillis()}.jpg"
            Result.success(
                EncodedUploadFile(
                    fileName = fileName.take(150),
                    mimeType = "image/jpeg",
                    sizeBytes = jpegBytes.size.toLong(),
                    base64Payload = encoded,
                    previewBitmap = scaled
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readPdfFromUri(context: Context, uri: Uri, fallbackName: String): Result<EncodedUploadFile> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Unable to read PDF file."))
            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Selected PDF file is empty."))
            }
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (encoded.length > MAX_BASE64_CHARS) {
                return Result.failure(IllegalArgumentException("Policy PDF must be under 300 KB for instant cloud delivery."))
            }
            val fileName = queryFileName(context, uri) ?: "${fallbackName}.pdf"
            Result.success(
                EncodedUploadFile(
                    fileName = fileName.take(150),
                    mimeType = "application/pdf",
                    sizeBytes = bytes.size.toLong(),
                    base64Payload = encoded
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToTempFile(context: Context, base64: String, fileName: String): File? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val dir = File(context.cacheDir, "docs").apply { mkdirs() }
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            File(dir, safeName).apply { writeBytes(bytes) }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } catch (_: Exception) {
            null
        }
    }
}
