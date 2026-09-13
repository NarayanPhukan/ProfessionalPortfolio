package com.narayan.portfolioadmin.data.notification

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "portfolio_admin_notifications"
        private const val KEY_PUSH_ENABLED = "push_enabled"
        private const val KEY_INQUIRY_ALERTS = "inquiry_alerts"
        private const val KEY_TRAFFIC_ALERTS = "traffic_alerts"
        private const val KEY_UPDATE_ALERTS = "update_alerts"
        private const val KEY_SOUND_VIBRATE = "sound_vibrate"
    }

    var isPushEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUSH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PUSH_ENABLED, value).apply()

    var isInquiryAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_INQUIRY_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_INQUIRY_ALERTS, value).apply()

    var isTrafficMilestonesEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRAFFIC_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_TRAFFIC_ALERTS, value).apply()

    var isAppUpdateAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_UPDATE_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_UPDATE_ALERTS, value).apply()

    var isSoundAndVibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_VIBRATE, value).apply()
}
