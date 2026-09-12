package com.narayan.portfolioadmin.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.narayan.portfolioadmin.data.model.AppUpdateInfo
import com.narayan.portfolioadmin.data.model.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val TAG = "UpdateManager"
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    suspend fun checkForUpdates(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val currentCode = getCurrentVersionCode(context)
            val currentName = getCurrentVersionName(context)

            val doc = firestore.collection("app_updates").document("latest").get().await()
            if (!doc.exists()) {
                return@withContext UpdateCheckResult.UpToDate(currentName)
            }

            val remoteCode = doc.getLong("version_code") ?: 1L
            val remoteName = doc.getString("version_name") ?: "1.0.0"
            val releaseNotes = doc.getString("release_notes") ?: "Bug fixes and performance improvements."
            val apkUrl = doc.getString("apk_url") ?: ""
            val forceUpdate = doc.getBoolean("force_update") ?: false
            val releasedAt = doc.getString("released_at") ?: ""

            if (remoteCode > currentCode && apkUrl.isNotBlank()) {
                val updateInfo = AppUpdateInfo(
                    versionCode = remoteCode,
                    versionName = remoteName,
                    releaseNotes = releaseNotes,
                    apkUrl = apkUrl,
                    forceUpdate = forceUpdate,
                    releasedAt = releasedAt
                )
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                UpdateCheckResult.UpToDate(currentName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult.Error(e.localizedMessage ?: "Failed to check for updates")
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()

            val apkFile = File(updatesDir, "portfolio_admin_update.apk")
            if (apkFile.exists()) apkFile.delete()

            // Open HTTP Connection with manual redirect following for CDNs/GitHub Releases
            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0
            while (true) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                    status == HttpURLConnection.HTTP_MOVED_PERM || 
                    status == HttpURLConnection.HTTP_SEE_OTHER || 
                    status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl.isNullOrBlank() || redirectCount++ > 7) {
                        throw IllegalStateException("Too many redirects or invalid Location header: $newUrl")
                    }
                    currentUrl = if (newUrl.startsWith("http")) newUrl else URL(url, newUrl).toString()
                } else if (status in 200..299) {
                    break
                } else {
                    throw IllegalStateException("Server returned HTTP $status")
                }
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8 * 1024)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileLength > 0) {
                    onProgress(totalBytesRead.toFloat() / fileLength.toFloat())
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        // Check Android 8.0+ Unknown App Sources Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }

    suspend fun publishUpdateInfo(info: AppUpdateInfo): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("app_updates").document("latest").set(
                mapOf(
                    "version_code" to info.versionCode,
                    "version_name" to info.versionName,
                    "release_notes" to info.releaseNotes,
                    "apk_url" to info.apkUrl,
                    "force_update" to info.forceUpdate,
                    "released_at" to info.releasedAt
                )
            ).await()
            Unit
        }
    }
}
