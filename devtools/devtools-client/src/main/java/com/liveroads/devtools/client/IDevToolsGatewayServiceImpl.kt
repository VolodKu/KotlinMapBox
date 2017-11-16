package com.liveroads.devtools.client

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.annotation.AnyThread
import android.support.annotation.MainThread
import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.api.IDevToolsService

internal class IDevToolsGatewayServiceImpl : IDevToolsGatewayService.Stub(), ServiceConnection {

    private lateinit var appContext: Context
    private var service: Service? = null
    private var devToolsService: IDevToolsService? = null
    private var bound = false

    @MainThread
    fun onCreate(service: Service) {
        this.service = service
        appContext = service.applicationContext

        Intent().let { intent ->
            intent.component = DevToolsService.COMPONENT_NAME
            bound = service.bindService(intent, this, Service.BIND_AUTO_CREATE)
        }
    }

    @MainThread
    fun onDestroy() {
        service!!.unbindService(this)
        service = null
        broadcastEnabledChanged()
    }

    @AnyThread
    override fun getEnabledState(): Int {
        val state = if (!bound) {
            DevToolsGatewayService.EnabledState.HARD_DISABLED
        } else if (devToolsService?.isEnabled ?: false) {
            DevToolsGatewayService.EnabledState.ENABLED
        } else {
            DevToolsGatewayService.EnabledState.DISABLED
        }
        return state.ordinal
    }

    override fun getDevToolsService(): IBinder? {
        return devToolsService?.asBinder()
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        devToolsService = DevToolsService.verifyDevToolsBinder(binder)
        broadcastEnabledChanged()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        devToolsService = null
        broadcastEnabledChanged()
    }

    private fun broadcastEnabledChanged() {
        val intent = Intent().apply {
            action = DevToolsService.ACTION_DEV_TOOLS_SETTING_CHANGED
        }
        appContext.sendBroadcast(intent, "com.liveroads.permission.INTERNAL")
    }

}
