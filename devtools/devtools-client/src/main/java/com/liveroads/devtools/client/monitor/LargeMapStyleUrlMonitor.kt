package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.client.DevToolsClient

open class LargeMapStyleUrlMonitor(devTools: DevToolsClient)
    : DevToolsStringSettingMonitor(devTools, KEY) {

    companion object {
        const val KEY = DevToolsService.KEY_LARGE_MAP_STYLE_URL
    }

}
