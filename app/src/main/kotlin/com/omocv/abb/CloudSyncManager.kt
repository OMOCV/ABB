package com.omocv.abb

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * Cloud Sync Manager - Completely rewritten for better reliability
 * Supports WebDAV and Google Drive with proper error handling and retry mechanism
 */
class CloudSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "CloudSyncManager"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val CONNECTION_TIMEOUT = 20000
        private const val READ_TIMEOUT = 20000
        private const val GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }

    // Sealed class for sync results
    sealed class SyncResult {
        data class Success(val provider: String, val fileId: String? = null, val message: String) : SyncResult()
        data class Failure(val provider: String, val error: String, val retryable: Boolean = false) : SyncResult()
        data class Progress(val provider: String, val message: String, val percentage: Int) : SyncResult()
    }

    // Provider configuration
    data class WebDavConfig(
        val url: String,
        val username: String = "",
        val password: String = ""
    )

    data class GoogleDriveConfig(
        val account: GoogleSignInAccount
    )

    /**
     * Sync file to WebDAV server with retry mechanism
     */
    suspend fun syncToWebDav(
        config: WebDavConfig,
        fileName: String,
        content: String,
        onProgress: (SyncResult.Progress) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting WebDAV sync for file: $fileName")

        var lastError: String? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                onProgress(SyncResult.Progress("WebDAV", "Connecting to server... (Attempt ${attempt + 1}/$MAX_RETRIES)", 10))

                val targetUrl = normalizeWebDavUrl(config.url, fileName)
                Log.d(TAG, "Target URL: $targetUrl")

                // Step 1: Ensure parent directories exist
                onProgress(SyncResult.Progress("WebDAV", "Checking directories...", 20))
                val dirCheckResult = ensureWebDavDirectories(config, targetUrl)
                if (dirCheckResult != null) {
                    return@withContext dirCheckResult
                }

                // Step 2: Check if file exists
                onProgress(SyncResult.Progress("WebDAV", "Checking if file exists...", 40))
                val fileExists = checkWebDavFileExists(config, targetUrl)

                // Step 3: Upload file
                onProgress(SyncResult.Progress("WebDAV", "Uploading file...", 60))
                val uploadResult = uploadToWebDav(config, targetUrl, content, fileExists)

                if (uploadResult is SyncResult.Success) {
                    onProgress(SyncResult.Progress("WebDAV", "Upload completed!", 100))
                    return@withContext uploadResult
                } else if (uploadResult is SyncResult.Failure && !uploadResult.retryable) {
                    return@withContext uploadResult
                }

                lastError = (uploadResult as? SyncResult.Failure)?.error ?: "Upload failed"

            } catch (e: Exception) {
                Log.e(TAG, "WebDAV sync attempt ${attempt + 1} failed", e)
                lastError = e.message ?: "Unknown error"

                if (attempt < MAX_RETRIES - 1) {
                    onProgress(SyncResult.Progress("WebDAV", "Retrying in ${RETRY_DELAY_MS / 1000}s...", 0))
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        SyncResult.Failure("WebDAV", "Failed after $MAX_RETRIES attempts: $lastError", false)
    }

    /**
     * Sync file to Google Drive with retry mechanism
     */
    suspend fun syncToGoogleDrive(
        config: GoogleDriveConfig,
        fileName: String,
        content: String,
        onProgress: (SyncResult.Progress) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting Google Drive sync for file: $fileName")

        var lastError: String? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                onProgress(SyncResult.Progress("Google Drive", "Authenticating... (Attempt ${attempt + 1}/$MAX_RETRIES)", 10))

                // Step 1: Get OAuth token
                val token = getGoogleAuthToken(config.account)
                if (token == null) {
                    return@withContext SyncResult.Failure("Google Drive", "Failed to obtain authentication token", false)
                }

                // Step 2: Search for existing file
                onProgress(SyncResult.Progress("Google Drive", "Searching for existing file...", 30))
                val existingFileId = searchGoogleDriveFile(token, fileName)

                // Step 3: Upload or update file
                if (existingFileId != null) {
                    onProgress(SyncResult.Progress("Google Drive", "Updating existing file...", 50))
                    val result = updateGoogleDriveFile(token, existingFileId, fileName, content)
                    if (result is SyncResult.Success) {
                        onProgress(SyncResult.Progress("Google Drive", "File updated!", 100))
                        return@withContext result
                    }
                    lastError = (result as? SyncResult.Failure)?.error
                } else {
                    onProgress(SyncResult.Progress("Google Drive", "Creating new file...", 50))
                    val result = createGoogleDriveFile(token, fileName, content)
                    if (result is SyncResult.Success) {
                        onProgress(SyncResult.Progress("Google Drive", "File created!", 100))
                        return@withContext result
                    }
                    lastError = (result as? SyncResult.Failure)?.error
                }

            } catch (e: Exception) {
                Log.e(TAG, "Google Drive sync attempt ${attempt + 1} failed", e)
                lastError = e.message ?: "Unknown error"

                if (attempt < MAX_RETRIES - 1) {
                    onProgress(SyncResult.Progress("Google Drive", "Retrying in ${RETRY_DELAY_MS / 1000}s...", 0))
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        SyncResult.Failure("Google Drive", "Failed after $MAX_RETRIES attempts: $lastError", false)
    }

    // ========================= WebDAV Helper Functions =========================

    private fun normalizeWebDavUrl(baseUrl: String, fileName: String): String {
        val cleanUrl = baseUrl.trimEnd('/')
        val encodedFileName = URLEncoder.encode(fileName, "UTF-8")
        return "$cleanUrl/$encodedFileName"
    }

    private suspend fun ensureWebDavDirectories(config: WebDavConfig, targetUrl: String): SyncResult.Failure? {
        return try {
            val url = URL(targetUrl)
            val path = url.path ?: return null
            val lastSlash = path.lastIndexOf('/')
            if (lastSlash <= 0) return null

            val dirPath = path.substring(0, lastSlash)
            if (dirPath.isBlank() || dirPath == "/") return null

            val segments = dirPath.split('/').filter { it.isNotBlank() }
            var cumulativePath = ""

            for (segment in segments) {
                cumulativePath += "/$segment"
                val dirUrl = URL(url.protocol, url.host, url.port, cumulativePath)

                val connection = dirUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "MKCOL"
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                connection.setRequestProperty("User-Agent", "ABB-CloudSync/2.0")

                if (config.username.isNotBlank()) {
                    val auth = "${config.username}:${config.password}"
                    val encoded = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
                    connection.setRequestProperty("Authorization", "Basic $encoded")
                }

                val responseCode = connection.responseCode
                connection.disconnect()

                // 201 = created, 405 = already exists, 200/204 = OK
                if (responseCode !in listOf(200, 201, 204, 405, 409)) {
                    if (responseCode == 401) {
                        return SyncResult.Failure("WebDAV", "Authentication failed - check username/password", false)
                    }
                    return SyncResult.Failure("WebDAV", "Failed to create directory: HTTP $responseCode", true)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring WebDAV directories", e)
            SyncResult.Failure("WebDAV", "Directory creation error: ${e.message}", true)
        }
    }

    private suspend fun checkWebDavFileExists(config: WebDavConfig, targetUrl: String): Boolean {
        return try {
            val url = URL(targetUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            if (config.username.isNotBlank()) {
                val auth = "${config.username}:${config.password}"
                val encoded = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
                connection.setRequestProperty("Authorization", "Basic $encoded")
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode == 200
        } catch (e: Exception) {
            Log.w(TAG, "Error checking WebDAV file existence", e)
            false
        }
    }

    private suspend fun uploadToWebDav(
        config: WebDavConfig,
        targetUrl: String,
        content: String,
        isUpdate: Boolean
    ): SyncResult {
        return try {
            val url = URL(targetUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.doOutput = true

            val contentBytes = content.toByteArray(Charsets.UTF_8)
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            connection.setRequestProperty("Content-Length", contentBytes.size.toString())
            connection.setRequestProperty("User-Agent", "ABB-CloudSync/2.0")

            if (config.username.isNotBlank()) {
                val auth = "${config.username}:${config.password}"
                val encoded = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
                connection.setRequestProperty("Authorization", "Basic $encoded")
            }

            // Write content
            connection.outputStream.use { it.write(contentBytes) }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage ?: ""

            Log.d(TAG, "WebDAV upload response: $responseCode - $responseMessage")

            connection.disconnect()

            when (responseCode) {
                in 200..299 -> {
                    val action = if (isUpdate) "updated" else "created"
                    SyncResult.Success("WebDAV", null, "File $action successfully at $targetUrl")
                }
                401 -> SyncResult.Failure("WebDAV", "Authentication failed", false)
                403 -> SyncResult.Failure("WebDAV", "Permission denied", false)
                404 -> SyncResult.Failure("WebDAV", "Path not found", true)
                507 -> SyncResult.Failure("WebDAV", "Insufficient storage", false)
                else -> SyncResult.Failure("WebDAV", "Upload failed: HTTP $responseCode - $responseMessage", true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "WebDAV upload error", e)
            SyncResult.Failure("WebDAV", "Upload error: ${e.message}", true)
        }
    }

    // ========================= Google Drive Helper Functions =========================

    private fun getGoogleAuthToken(account: GoogleSignInAccount): String? {
        return try {
            val androidAccount = account.account ?: return null
            GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$GOOGLE_DRIVE_SCOPE")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Google auth token", e)
            null
        }
    }

    private suspend fun searchGoogleDriveFile(token: String, fileName: String): String? {
        return try {
            val query = URLEncoder.encode("name='$fileName' and trashed=false", "UTF-8")
            val searchUrl = URL("https://www.googleapis.com/drive/v3/files?q=$query&spaces=drive&fields=files(id,name)&pageSize=1")

            val connection = searchUrl.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                connection.disconnect()

                val json = JSONObject(response)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val fileId = files.getJSONObject(0).getString("id")
                    Log.d(TAG, "Found existing file with ID: $fileId")
                    return fileId
                }
            }
            connection.disconnect()
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error searching Google Drive file", e)
            null
        }
    }

    private suspend fun createGoogleDriveFile(token: String, fileName: String, content: String): SyncResult {
        return try {
            val boundary = "===boundary==="
            val metadata = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "text/plain")
            }

            val multipartBody = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString())
                append("\r\n--$boundary\r\n")
                append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
                append(content)
                append("\r\n--$boundary--")
            }

            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")

            OutputStreamWriter(connection.outputStream).use { it.write(multipartBody) }

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            }

            connection.disconnect()

            Log.d(TAG, "Google Drive create response: $responseCode")

            when (responseCode) {
                in 200..299 -> {
                    val json = JSONObject(response)
                    val fileId = json.optString("id", "")
                    SyncResult.Success("Google Drive", fileId, "File created successfully")
                }
                401 -> SyncResult.Failure("Google Drive", "Authentication expired - please sign in again", false)
                403 -> SyncResult.Failure("Google Drive", "Permission denied - check Drive access", false)
                else -> {
                    val errorMsg = try {
                        val json = JSONObject(response)
                        json.optJSONObject("error")?.optString("message", "Unknown error")
                    } catch (e: Exception) {
                        "HTTP $responseCode"
                    }
                    SyncResult.Failure("Google Drive", "Upload failed: $errorMsg", true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Google Drive file", e)
            SyncResult.Failure("Google Drive", "Error: ${e.message}", true)
        }
    }

    private suspend fun updateGoogleDriveFile(token: String, fileId: String, fileName: String, content: String): SyncResult {
        return try {
            val boundary = "===boundary==="
            val metadata = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "text/plain")
            }

            val multipartBody = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString())
                append("\r\n--$boundary\r\n")
                append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
                append(content)
                append("\r\n--$boundary--")
            }

            val url = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "PATCH"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")

            OutputStreamWriter(connection.outputStream).use { it.write(multipartBody) }

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            }

            connection.disconnect()

            Log.d(TAG, "Google Drive update response: $responseCode")

            when (responseCode) {
                in 200..299 -> {
                    SyncResult.Success("Google Drive", fileId, "File updated successfully")
                }
                401 -> SyncResult.Failure("Google Drive", "Authentication expired - please sign in again", false)
                403 -> SyncResult.Failure("Google Drive", "Permission denied - check Drive access", false)
                404 -> SyncResult.Failure("Google Drive", "File not found - will create new", true)
                else -> {
                    val errorMsg = try {
                        val json = JSONObject(response)
                        json.optJSONObject("error")?.optString("message", "Unknown error")
                    } catch (e: Exception) {
                        "HTTP $responseCode"
                    }
                    SyncResult.Failure("Google Drive", "Update failed: $errorMsg", true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Google Drive file", e)
            SyncResult.Failure("Google Drive", "Error: ${e.message}", true)
        }
    }
}
