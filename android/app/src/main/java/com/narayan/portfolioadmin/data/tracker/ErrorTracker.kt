package com.narayan.portfolioadmin.data.tracker

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.narayan.portfolioadmin.data.model.ErrorReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

object ErrorTracker {
    private const val TAG = "ErrorTracker"
    private const val PREFS_NAME = "crash_telemetry"
    private const val KEY_HAS_PENDING_CRASH = "has_pending_crash"
    private const val KEY_CRASH_TITLE = "crash_title"
    private const val KEY_CRASH_STACKTRACE = "crash_stacktrace"
    private const val KEY_CRASH_TIMESTAMP = "crash_timestamp"
    private const val KEY_CRASH_THREAD = "crash_thread"
    private const val MAX_BREADCRUMBS = 30

    private val breadcrumbs = ConcurrentLinkedDeque<String>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(application: Application) {
        try {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
            logBreadcrumb("ErrorTracker initialized")

            val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                handleUncaughtException(application, thread, throwable, originalHandler)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ErrorTracker", e)
        }
    }

    private fun handleUncaughtException(
        context: Context,
        thread: Thread,
        throwable: Throwable,
        originalHandler: Thread.UncaughtExceptionHandler?
    ) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            val crashTitle = "${throwable.javaClass.simpleName}: ${throwable.message ?: "Fatal Exception"}"

            // 1. Record to Crashlytics
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("thread_name", thread.name)
            crashlytics.recordException(throwable)

            // 2. Persist emergency crash record in SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            prefs.edit()
                .putBoolean(KEY_HAS_PENDING_CRASH, true)
                .putString(KEY_CRASH_TITLE, crashTitle)
                .putString(KEY_CRASH_STACKTRACE, stackTrace)
                .putString(KEY_CRASH_TIMESTAMP, timestamp)
                .putString(KEY_CRASH_THREAD, thread.name)
                .commit() // Synchronous commit before termination

            // 3. Attempt synchronous direct write to Firestore
            val report = ErrorReport(
                id = UUID.randomUUID().toString(),
                title = crashTitle,
                description = "Fatal unhandled crash on thread [${thread.name}]",
                error_type = "CRASH",
                stack_trace = stackTrace,
                device_info = getDeviceInfo(),
                app_version = "1.0",
                screen_name = "Process Crash",
                created_at = timestamp,
                status = "new"
            )
            firestore.collection("error_reports").document(report.id).set(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling crash", e)
        } finally {
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logBreadcrumb(message: String) {
        val entry = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}: $message"
        breadcrumbs.addLast(entry)
        while (breadcrumbs.size > MAX_BREADCRUMBS) {
            breadcrumbs.pollFirst()
        }
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (_: Exception) {}
    }

    fun logNonFatal(tag: String, message: String, throwable: Throwable? = null) {
        logBreadcrumb("[$tag] $message")
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log("[$tag] $message")
            if (throwable != null) {
                crashlytics.recordException(throwable)
            }
        } catch (_: Exception) {}

        if (throwable != null) {
            scope.launch {
                try {
                    val sw = StringWriter()
                    throwable.printStackTrace(PrintWriter(sw))
                    val report = ErrorReport(
                        id = UUID.randomUUID().toString(),
                        title = "Non-Fatal: $tag - ${throwable.message ?: message}",
                        description = message,
                        error_type = "NON_FATAL",
                        stack_trace = sw.toString(),
                        device_info = getDeviceInfo(),
                        app_version = "1.0",
                        screen_name = tag,
                        created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                        status = "new"
                    )
                    firestore.collection("error_reports").document(report.id).set(report).await()
                } catch (_: Exception) {}
            }
        }
    }

    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT}, Brand: ${Build.BRAND})"
    }

    suspend fun checkAndUploadPendingCrash(context: Context): ErrorReport? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_PENDING_CRASH, false)) return null

        val title = prefs.getString(KEY_CRASH_TITLE, "App Crash") ?: "App Crash"
        val stackTrace = prefs.getString(KEY_CRASH_STACKTRACE, "") ?: ""
        val timestamp = prefs.getString(KEY_CRASH_TIMESTAMP, "") ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val thread = prefs.getString(KEY_CRASH_THREAD, "main") ?: "main"

        val report = ErrorReport(
            id = UUID.randomUUID().toString(),
            title = title,
            description = "Auto-detected crash on thread [$thread] from previous session",
            error_type = "CRASH",
            stack_trace = stackTrace,
            device_info = getDeviceInfo(),
            app_version = "1.0",
            screen_name = "Previous Session",
            created_at = timestamp,
            status = "new"
        )

        return try {
            firestore.collection("error_reports").document(report.id).set(report).await()
            prefs.edit().clear().apply()
            report
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload pending crash", e)
            null
        }
    }

    suspend fun submitUserReport(
        title: String,
        description: String,
        screenName: String,
        includeDiagnostics: Boolean
    ): Result<Unit> = runCatching {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val diagnostics = if (includeDiagnostics) {
            "Recent Events:\n" + breadcrumbs.joinToString("\n")
        } else ""

        val report = ErrorReport(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            error_type = "USER_REPORT",
            stack_trace = diagnostics,
            device_info = if (includeDiagnostics) getDeviceInfo() else "",
            app_version = "1.0",
            screen_name = screenName,
            created_at = timestamp,
            status = "new"
        )

        firestore.collection("error_reports").document(report.id).set(report).await()
        FirebaseCrashlytics.getInstance().log("USER_REPORT: ${report.title}")
    }
}
