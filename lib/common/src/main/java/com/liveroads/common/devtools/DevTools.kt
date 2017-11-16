package com.liveroads.common.devtools

import android.content.Context
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.client.DevToolsClient

private var _devTools: DevToolsClient? = null

fun init(context: Context) {
    _devTools = DevToolsClient(context, obtainLogger("DevTools"))
}

val devTools: DevToolsClient
    get() = _devTools as DevToolsClient
