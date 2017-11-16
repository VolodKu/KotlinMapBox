package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.client.DevToolsClient

open class DevToolsBooleanSettingMonitor(devTools: DevToolsClient, val key: String, defaultValue: Boolean)
    : DevToolsSettingMonitor<Boolean>(devTools, defaultValue) {

    override fun loadValue() = devTools.getBooleanSetting(key, defaultValue)

}
