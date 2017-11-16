package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.client.DevToolsClient

open class LargeMapCenterAsLocationEnabledMonitor(devTools: DevToolsClient)
    : DevToolsBooleanSettingMonitor(devTools, KEY, DEFAULT_VALUE) {

    companion object {
        const val KEY = DevToolsService.KEY_USE_LARGE_MAP_CENTER_AS_LOCATION
        const val DEFAULT_VALUE = false
    }

}
