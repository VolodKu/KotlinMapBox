package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.client.DevToolsClient

open class DevToolsEnabledMonitor(devTools: DevToolsClient) : DevToolsSettingMonitor<Boolean>(devTools, false) {

    override fun loadValue() = devTools.isEnabled

}
