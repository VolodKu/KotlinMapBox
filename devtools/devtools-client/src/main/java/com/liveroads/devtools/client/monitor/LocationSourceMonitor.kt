package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.client.DevToolsClient

open class LocationSourceMonitor(devTools: DevToolsClient)
    : DevToolsSettingMonitor<DevToolsService.LocationSource?>(devTools, null) {

    override fun loadValue(): DevToolsService.LocationSource? {
        val valueStr = devTools.getStringSetting(KEY, null) ?: return defaultValue
        return try {
            DevToolsService.LocationSource.valueOf(valueStr)
        } catch (_: IllegalArgumentException) {
            defaultValue
        }
    }

    companion object {
        const val KEY = DevToolsService.KEY_LOCATION_SOURCE
    }

}
