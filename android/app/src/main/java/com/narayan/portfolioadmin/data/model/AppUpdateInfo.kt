package com.narayan.portfolioadmin.data.model

data class AppUpdateInfo(
    val versionCode: Long = 1,
    val versionName: String = "1.0.0",
    val releaseNotes: String = "",
    val apkUrl: String = "",
    val forceUpdate: Boolean = false,
    val releasedAt: String = ""
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
