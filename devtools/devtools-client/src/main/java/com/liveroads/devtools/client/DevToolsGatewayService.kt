package com.liveroads.devtools.client

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DevToolsGatewayService : Service() {

    private val binder = IDevToolsGatewayServiceImpl()

    override fun onCreate() {
        super.onCreate()
        binder.onCreate(this)
    }

    override fun onDestroy() {
        binder.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    enum class EnabledState {
        ENABLED,
        DISABLED,
        HARD_DISABLED,
    }

}
