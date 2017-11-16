package com.liveroads.devtools.client.monitor

import android.support.annotation.MainThread
import com.liveroads.devtools.client.DevToolsClient

abstract class DevToolsSettingMonitor<T>(val devTools: DevToolsClient, protected val defaultValue: T) {

    var value = defaultValue
        private set

    private val listeners = mutableListOf<Listener>()

    @MainThread
    fun start() {
        devTools.addListener(devToolsClientListener)
        updateValue()
    }

    @MainThread
    fun stop() {
        devTools.removeListener(devToolsClientListener)
    }

    @MainThread
    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    @MainThread
    fun removeListener(listener: Listener) {
        for (i in listeners.size - 1 downTo 0) {
            if (listeners[i] == listener) {
                listeners.removeAt(i)
                break
            }
        }
    }

    protected abstract fun loadValue(): T

    @MainThread
    private fun updateValue() {
        val newValue = loadValue()
        if (newValue != value) {
            value = newValue
            onValueChanged()
            listeners.forEach {
                it.onValueChanged(this)
            }
        }
    }

    @MainThread
    open fun onValueChanged() {
    }

    private val devToolsClientListener = object : DevToolsClient.Listener {

        override fun onDevSettingChanged(dt: DevToolsClient) {
            updateValue()
        }

    }

    interface Listener {

        @MainThread
        fun onValueChanged(monitor: DevToolsSettingMonitor<*>)

    }

}
