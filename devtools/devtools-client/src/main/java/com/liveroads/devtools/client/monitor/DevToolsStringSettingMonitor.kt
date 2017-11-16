package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.client.DevToolsClient

open class DevToolsStringSettingMonitor(devTools: DevToolsClient, val key: String, defaultValue: String? = null)
    : DevToolsSettingMonitor<String?>(devTools, defaultValue) {

    override fun loadValue() = devTools.getStringSetting(key, defaultValue)

}
