package com.liveroads.devtools

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import com.liveroads.devtools.api.DevToolsService.ACTION_DEV_TOOLS_SETTING_CHANGED
import com.liveroads.devtools.api.IDevToolsService

class DevToolsService : Service() {

    private val binder = DevToolsServiceImpl()

    override fun onCreate() {
        super.onCreate()
        binder.onCreate(applicationContext)
    }

    override fun onDestroy() {
        binder.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

}

private class DevToolsServiceImpl : IDevToolsService.Stub(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var privatePrefs: SharedPreferences

    fun onCreate(context: Context) {
        this.context = context

        prefs = context.getSharedPreferences("DevTools", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        privatePrefs = context.getSharedPreferences("DevTools-Private", Context.MODE_PRIVATE)
        privatePrefs.registerOnSharedPreferenceChangeListener(this)
    }

    fun onDestroy() {
        privatePrefs.unregisterOnSharedPreferenceChangeListener(this)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        sendDevToolsSettingChangedBroadcast()
    }

    private fun sendDevToolsSettingChangedBroadcast() {
        val intent = Intent().apply {
            action = ACTION_DEV_TOOLS_SETTING_CHANGED
        }
        context.sendBroadcast(intent, "com.liveroads.permission.INTERNAL")
    }

    override fun getVersion() = 1
    override fun isFeatureSupported(featureName: String?) = false

    override fun isEnabled() = privatePrefs.getBoolean(KEY_ENABLED, true)
    override fun setEnabled(enabled: Boolean) {
        privatePrefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override fun call(method: String?, arg: String?, args: Bundle?) = null

    override fun clear() {
        prefs.edit().clear().apply()
        sendDevToolsSettingChangedBroadcast()
    }

    override fun remove(key: String?) {
        if (key != null) prefs.edit().remove(key).apply()
    }

    override fun setBoolean(key: String?, value: Boolean) {
        if (key != null) prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String?, defaultValue: Boolean): Boolean {
        return if (key == null) defaultValue else prefs.getBoolean(key, defaultValue)
    }

    override fun setFloat(key: String?, value: Float) {
        if (key != null) prefs.edit().putFloat(key, value).apply()
    }

    override fun getFloat(key: String?, defaultValue: Float): Float {
        return if (key == null) defaultValue else prefs.getFloat(key, defaultValue)
    }

    override fun setInt(key: String?, value: Int) {
        if (key != null) prefs.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String?, defaultValue: Int): Int {
        return if (key == null) defaultValue else prefs.getInt(key, defaultValue)
    }

    override fun setLong(key: String?, value: Long) {
        if (key != null) prefs.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String?, defaultValue: Long): Long {
        return if (key == null) defaultValue else prefs.getLong(key, defaultValue)
    }

    override fun setString(key: String?, value: String?) {
        if (key != null) prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String?, defaultValue: String?): String? {
        return if (key == null) defaultValue else prefs.getString(key, defaultValue)
    }

    companion object {
        const val KEY_ENABLED = "enabled"
    }

}
