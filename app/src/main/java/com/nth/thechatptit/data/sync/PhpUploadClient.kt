package com.nth.thechatptit.data.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.nth.thechatptit.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class UploadApiException(
    message: String,
    val retryable: Boolean
) : IOException(message)

internal fun uploadApiFileId(value: String?, baseUrl: String): String? {
    if (value == null) return null
    val normalizedBaseUrl = baseUrl.trimEnd('/')
    val path = if (value.startsWith("$normalizedBaseUrl/")) {
        value.removePrefix(normalizedBaseUrl)
    } else {
        value
    }
    return Regex("^/api/v1/files/([a-f0-9]{32})$").matchEntire(path)?.groupValues?.get(1)
}

@Singleton
class PhpUploadClient @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val baseUrl = BuildConfig.UPLOAD_API_BASE_URL.trimEnd('/')

    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://", ignoreCase = true)

    fun absoluteUrl(path: String): String {
        if (!isApiFilePath(path)) throw UploadApiException("Đường dẫn tệp không hợp lệ.", retryable = false)
        return "$baseUrl/${path.removePrefix("/")}"
    }

    fun isApiFilePath(value: String?): Boolean = fileId(value) != null

    suspend fun uploadFile(userId: String, kind: String, file: File): String {
        if (!file.isFile || file.length() <= 0L) {
            throw UploadApiException("Không tìm thấy tệp cần tải lên.", retryable = false)
        }
        return withContext(Dispatchers.IO) {
            val normalizedImage = normalizeImage(file)
            try {
                upload(userId, kind, "upload.jpg", "image/jpeg", normalizedImage.length()) { output ->
                    FileInputStream(normalizedImage).use { input -> input.copyTo(output) }
                }
            } finally {
                if (normalizedImage != file) normalizedImage.delete()
            }
        }
    }

    suspend fun uploadBytes(userId: String, kind: String, fileName: String, bytes: ByteArray): String {
        if (bytes.isEmpty()) throw UploadApiException("Tệp tải lên đang rỗng.", retryable = false)
        return upload(userId, kind, fileName, "application/json", bytes.size.toLong()) { output ->
            output.write(bytes)
        }
    }

    suspend fun downloadBytes(userId: String, path: String, maxBytes: Int): ByteArray =
        withContext(Dispatchers.IO) {
            val id = fileId(path) ?: throw UploadApiException("Đường dẫn tệp không hợp lệ.", retryable = false)
            val connection = openDownloadWithRefresh(userId, id)
            try {
                val status = connection.responseCode
                if (status !in 200..299) throw responseError(connection, status)
                connection.inputStream.use { input -> readBounded(input, maxBytes) }
            } finally {
                connection.disconnect()
            }
        }

    suspend fun downloadImage(userId: String, path: String): String = withContext(Dispatchers.IO) {
        val id = fileId(path) ?: throw UploadApiException("Đường dẫn ảnh không hợp lệ.", retryable = false)
        val cacheDirectory = File(appContext.cacheDir, "php_uploads")
        if (!cacheDirectory.isDirectory && !cacheDirectory.mkdirs()) {
            throw UploadApiException("Không thể tạo bộ nhớ đệm ảnh.", retryable = true)
        }
        cacheDirectory.listFiles()
            ?.firstOrNull { it.nameWithoutExtension == id && it.isFile && it.length() > 0L }
            ?.let { return@withContext it.absolutePath }

        val connection = openDownloadWithRefresh(userId, id)
        var temporaryFile: File? = null
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw responseError(connection, status)
            val mimeType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            val extension = when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> throw UploadApiException("API không trả về định dạng ảnh được hỗ trợ.", retryable = false)
            }
            val maxBytes = 10 * 1024 * 1024
            if (connection.contentLengthLong > maxBytes) {
                throw UploadApiException("Ảnh tải xuống vượt quá giới hạn.", retryable = false)
            }

            val destination = File(cacheDirectory, "$id.$extension")
            temporaryFile = File.createTempFile("$id-", ".tmp", cacheDirectory)
            connection.inputStream.use { input ->
                temporaryFile.outputStream().use { output -> copyBounded(input, output, maxBytes) }
            }
            if (!temporaryFile.renameTo(destination)) {
                temporaryFile.copyTo(destination, overwrite = true)
                temporaryFile.delete()
            }
            destination.absolutePath
        } finally {
            temporaryFile?.delete()
            connection.disconnect()
        }
    }

    private suspend fun upload(
        userId: String,
        kind: String,
        fileName: String,
        mimeType: String,
        contentLength: Long,
        writeContent: (OutputStream) -> Unit
    ): String = withContext(Dispatchers.IO) {
        require(kind == "avatar" || kind == "post_image" || kind == "route") {
            "Loại tệp tải lên không hợp lệ."
        }
        val boundary = "KaiWalker-${UUID.randomUUID()}"
        val kindPart = "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"kind\"\r\n\r\n$kind\r\n"
        val filePart = "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n" +
            "Content-Type: $mimeType\r\n\r\n"
        val ending = "\r\n--$boundary--\r\n"
        val kindBytes = kindPart.toByteArray(Charsets.UTF_8)
        val fileHeaderBytes = filePart.toByteArray(Charsets.UTF_8)
        val endingBytes = ending.toByteArray(Charsets.UTF_8)
        for (attempt in 0..1) {
            val token = idToken(userId, forceRefresh = attempt == 1)
            val connection = (URL("$baseUrl/api/v1/uploads").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setFixedLengthStreamingMode(kindBytes.size + fileHeaderBytes.size + contentLength + endingBytes.size)
            }

            try {
                connection.outputStream.use { output ->
                    output.write(kindBytes)
                    output.write(fileHeaderBytes)
                    writeContent(output)
                    output.write(endingBytes)
                }
                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_UNAUTHORIZED && attempt == 0) continue
                val responseBody = readResponse(connection, status)
                if (status !in 200..299) throw responseError(status, responseBody)

                val response = runCatching { JSONObject(responseBody) }.getOrElse {
                    throw UploadApiException("Phản hồi từ upload API không hợp lệ.", retryable = true)
                }
                val data = response.optJSONObject("data")
                    ?: throw UploadApiException("Upload API thiếu thông tin tệp.", retryable = true)
                val path = data.optString("path")
                if (!response.optBoolean("ok") || fileId(path) == null || data.optString("ownerUid") != userId) {
                    throw UploadApiException("Upload API trả về thông tin không hợp lệ.", retryable = true)
                }
                return@withContext path
            } finally {
                connection.disconnect()
            }
        }
        throw UploadApiException("Không thể xác thực upload với Firebase.", retryable = false)
    }

    private fun openDownload(id: String, token: String): HttpURLConnection =
        (URL("$baseUrl/api/v1/files/$id").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json, image/jpeg, image/png, image/webp")
        }

    private suspend fun openDownloadWithRefresh(userId: String, id: String): HttpURLConnection {
        var connection = openDownload(id, idToken(userId))
        if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            connection.disconnect()
            connection = openDownload(id, idToken(userId, forceRefresh = true))
        }
        return connection
    }

    private fun normalizeImage(file: File): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw UploadApiException("Không thể đọc tệp ảnh đã chọn.", retryable = false)
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: throw UploadApiException("Không thể đọc tệp ảnh đã chọn.", retryable = false)

        val oriented = try {
            val orientation = ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation == 0f) decoded else {
                Bitmap.createBitmap(
                    decoded,
                    0,
                    0,
                    decoded.width,
                    decoded.height,
                    Matrix().apply { postRotate(rotation) },
                    true
                )
            }
        } catch (_: Exception) {
            decoded
        }
        val scale = minOf(
            1f,
            MAX_IMAGE_DIMENSION.toFloat() / maxOf(oriented.width, oriented.height)
        )
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                oriented,
                (oriented.width * scale).toInt().coerceAtLeast(1),
                (oriented.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            oriented
        }

        val output = File.createTempFile("kaiwalker-upload-", ".jpg", appContext.cacheDir)
        try {
            FileOutputStream(output).use { stream ->
                if (!resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    throw UploadApiException("Không thể xử lý tệp ảnh.", retryable = false)
                }
            }
            if (output.length() > MAX_IMAGE_BYTES) {
                throw UploadApiException("Ảnh vượt quá giới hạn sau khi xử lý.", retryable = false)
            }
            return output
        } catch (exception: Exception) {
            output.delete()
            if (exception is UploadApiException) throw exception
            throw UploadApiException("Không thể xử lý tệp ảnh.", retryable = false)
        } finally {
            if (resized !== oriented) resized.recycle()
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }

    private fun idToken(userId: String, forceRefresh: Boolean = false): String {
        val user = auth?.currentUser
            ?: throw UploadApiException("Đăng nhập để đồng bộ tệp.", retryable = false)
        if (user.uid != userId) {
            throw UploadApiException("Tài khoản đã thay đổi, hãy đồng bộ lại.", retryable = false)
        }
        return try {
            Tasks.await(user.getIdToken(forceRefresh)).token
                ?: throw UploadApiException("Không lấy được Firebase ID token.", retryable = true)
        } catch (exception: UploadApiException) {
            throw exception
        } catch (_: Exception) {
            throw UploadApiException("Không thể xác thực với Firebase.", retryable = true)
        }
    }

    private fun fileId(value: String?): String? = uploadApiFileId(value, baseUrl)

    private fun responseError(connection: HttpURLConnection, status: Int): UploadApiException =
        responseError(status, readResponse(connection, status))

    private fun responseError(status: Int, body: String): UploadApiException {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "Upload API trả về lỗi HTTP $status."
        return UploadApiException(
            message = message,
            retryable = status == 408 || status == 429 || status >= 500
        )
    }

    private fun readResponse(connection: HttpURLConnection, status: Int): String {
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray =
        ByteArrayOutputStream().use { output ->
            copyBounded(input, output, maxBytes)
            output.toByteArray()
        }

    private fun copyBounded(input: InputStream, output: OutputStream, maxBytes: Int) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                throw UploadApiException("Tệp tải xuống vượt quá giới hạn.", retryable = false)
            }
            output.write(buffer, 0, count)
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 15_000
        private const val READ_TIMEOUT_MILLIS = 60_000
        private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        private const val MAX_IMAGE_DIMENSION = 1_600
        private const val JPEG_QUALITY = 88
    }
}
