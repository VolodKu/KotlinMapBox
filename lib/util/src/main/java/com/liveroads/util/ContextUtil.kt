package com.liveroads.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection

fun Context.startServiceOrThrow(intent: Intent) {
    startService(intent) ?: throw ServiceNotFoundException("service not found: $intent")
}

fun Context.bindServiceOrThrow(intent: Intent, sc: ServiceConnection, flags: Int = Service.BIND_AUTO_CREATE) {
    if (!bindService(intent, sc, flags)) {
        throw ServiceNotFoundException("service not found: $intent")
    }
}

class ServiceNotFoundException(message: String) : RuntimeException(message)
