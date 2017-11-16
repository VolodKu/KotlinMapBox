package com.liveroads.devtools.client

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.RemoteException
import android.support.annotation.AnyThread
import android.support.annotation.MainThread
import com.liveroads.devtools.api.DevToolsService
import com.liveroads.devtools.api.IDevToolsService
import com.liveroads.devtools.client.DevToolsGatewayService.EnabledState
import com.liveroads.util.log.*
import java.util.concurrent.CopyOnWriteArrayList

class DevToolsClient(val context: Context, val logger: Logger) {

    var isEnabled: Boolean
        get() = (enabledState == EnabledState.ENABLED)
        set(value) {
            try {
                devToolsService?.setEnabled(value)
            } catch (e: RemoteException) {
                logger.w(e, "setEnabled() failed")
            }
        }

    private val serviceConnection = DevToolsGatewayServiceConnection()
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val mainHandler = MainHandler(this)
    private var service: IDevToolsGatewayService? = null
    private var devToolsService: IDevToolsService? = null
    private var bound = true

    init {
        Intent().let { intent ->
            intent.setClass(context, DevToolsGatewayService::class.java)
            if (!context.bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE)) {
                throw RuntimeException("service not found: $intent")
            }
        }
        IntentFilter().let { filter ->
            filter.addAction(DevToolsService.ACTION_DEV_TOOLS_SETTING_CHANGED)
            context.registerReceiver(DevToolsBroadcastReceiver(), filter, "com.liveroads.permission.INTERNAL", null)
        }
    }

    @AnyThread
    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    @AnyThread
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    @AnyThread
    fun getBooleanSetting(key: String, defaultValue: Boolean): Boolean {
        return isEnabled && try {
            devToolsService?.getBoolean(key, defaultValue) ?: defaultValue
        } catch (e: RemoteException) {
            logger.w(e, "getBoolean() failed")
            defaultValue
        }
    }

    @AnyThread
    fun setBooleanSetting(key: String, value: Boolean) {
        try {
            devToolsService?.setBoolean(key, value)
        } catch (e: RemoteException) {
            logger.w(e, "setBoolean() failed")
        }
    }

    @AnyThread
    fun getStringSetting(key: String, defaultValue: String? = null): String? {
        return if (!isEnabled) null else try {
            devToolsService?.getString(key, defaultValue) ?: defaultValue
        } catch (e: RemoteException) {
            logger.w(e, "getString() failed")
            defaultValue
        }
    }

    @AnyThread
    fun setStringSetting(key: String, value: String?) {
        try {
            devToolsService?.setString(key, value)
        } catch (e: RemoteException) {
            logger.w(e, "setString() failed")
        }
    }

    @AnyThread
    fun clear() {
        try {
            devToolsService?.clear()
        } catch (e: RemoteException) {
            logger.w(e, "clear() failed")
        }
    }

    @MainThread
    private fun onDevSettingChanged() {
        if (bound && enabledState == EnabledState.HARD_DISABLED) {
            context.unbindService(serviceConnection)
            service = null
            devToolsService = null
            bound = false
        } else if (devToolsService == null) {
            devToolsService = service?.devToolsService?.let {
                IDevToolsService.Stub.asInterface(it)
            }
        }
        listeners.iterator().forEach {
            it.onDevSettingChanged(this)
        }
    }

    private val enabledState: EnabledState
        get() {
            val state = try {
                service?.enabledState
            } catch (e: RemoteException) {
                logger.w(e, "enabledState failed")
                null
            }
            return if (state == null) {
                EnabledState.DISABLED
            } else {
                EnabledState.values()[state]
            }
        }

    private inner class DevToolsGatewayServiceConnection : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            IDevToolsGatewayService.Stub.asInterface(binder).let {
                service = it
                devToolsService = it.devToolsService?.let {
                    IDevToolsService.Stub.asInterface(it)
                }
            }
            logger.d("DevToolsGatewayService connected: enabled=%s", isEnabled)
            mainHandler.dispatchOnDevSettingChanged()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            logger.d("DevToolsGatewayService disconnected")
            service = null
            devToolsService = null
            mainHandler.dispatchOnDevSettingChanged()
        }

    }

    private inner class DevToolsBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            mainHandler.dispatchOnDevSettingChanged()
        }

    }

    interface Listener {

        @MainThread
        fun onDevSettingChanged(dt: DevToolsClient)

    }

    private class MainHandler(val dt: DevToolsClient) : Handler(Looper.getMainLooper()) {

        companion object {
            enum class Op {
                ON_DEV_SETTING_CHANGED,
            }
        }

        fun dispatchOnDevSettingChanged() {
            sendEmptyMessage(Op.ON_DEV_SETTING_CHANGED.ordinal)
        }

        override fun handleMessage(message: Message) {
            val op = Op.values()[message.what]
            when (op) {
                Op.ON_DEV_SETTING_CHANGED -> dt.onDevSettingChanged()
            }
        }

    }

}
