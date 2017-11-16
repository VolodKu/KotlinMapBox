package com.liveroads.devtools.client.monitor

import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.client.DevToolsClient

open class PipMapStyleUrlMonitor(devTools: DevToolsClient)
    : DevToolsStringSettingMonitor(devTools, KEY) {

    companion object {
        const val KEY = DevToolsService.KEY_PIP_MAP_STYLE_URL
    }

}
