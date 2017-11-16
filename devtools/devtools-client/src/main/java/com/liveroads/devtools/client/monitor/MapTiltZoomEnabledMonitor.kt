package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.client.DevToolsClient

open class MapTiltZoomEnabledMonitor(devTools: DevToolsClient)
    : DevToolsBooleanSettingMonitor(devTools, KEY, DEFAULT_VALUE) {

    companion object {
        const val KEY = DevToolsService.KEY_MAP_TILT_ZOOM_VISIBLE
        const val DEFAULT_VALUE = false
    }

}
