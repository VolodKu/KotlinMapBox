package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.client.DevToolsClient

open class MapGridLinesEnabledMonitor(devTools: DevToolsClient)
    : DevToolsBooleanSettingMonitor(devTools, KEY, DEFAULT_VALUE) {

    companion object {
        const val KEY = DevToolsService.KEY_MAP_GRID_LINES_ENABLED
        const val DEFAULT_VALUE = false
    }

}
