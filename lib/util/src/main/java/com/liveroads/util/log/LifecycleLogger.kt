package com.liveroads.util.log

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

fun Logger.logLifecycle(message: String, vararg args: Any?) {
    _log(LogLevel.VERBOSE, null, null, message, args)
}

fun Logger.onReceive(context: Context?, intent: Intent?) {
    logLifecycle("onReceive() context=%s intent=%s", context, intent)
}

fun Logger.onCreate() {
    logLifecycle("onCreate()")
}

fun Logger.onCreate(savedInstanceState: Bundle?) {
    logLifecycle("onCreate() savedInstanceState=%s", savedInstanceState)
}

fun Logger.onCreate(db: SQLiteDatabase?) {
    logLifecycle("onCreate() db=%s", db)
}

fun Logger.onDestroy() {
    logLifecycle("onDestroy()")
}

fun Logger.onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
        savedInstanceState: Bundle?) {
    logLifecycle("onCreateView() inflater=%s container=%s savedInstanceState=%s",
            inflater, container, savedInstanceState)
}

fun Logger.onViewCreated(view: View?, savedInstanceState: Bundle?) {
    logLifecycle("onViewCreated() view=%s savedInstanceState=%s", view, savedInstanceState)
}

fun Logger.onDestroyView() {
    logLifecycle("onDestroyView()")
}

fun Logger.onActivityCreated(savedInstanceState: Bundle?) {
    logLifecycle("onActivityCreated() savedInstanceState=%s", savedInstanceState)
}

fun Logger.onViewStateRestored(savedInstanceState: Bundle?) {
    logLifecycle("onViewStateRestored() savedInstanceState=%s", savedInstanceState)
}

fun Logger.onStart() {
    logLifecycle("onStart()")
}

fun Logger.onStop() {
    logLifecycle("onStop()")
}

fun Logger.onResume() {
    logLifecycle("onResume()")
}

fun Logger.onPause() {
    logLifecycle("onPause()")
}

fun Logger.onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    logLifecycle("onActivityResult() requestCode=%d resultCode=%d data=%s",
            requestCode, resultCode, data)
}

fun Logger.onStartCommand(intent: Intent?, flags: Int, startId: Int) {
    logLifecycle("onStartCommand() intent=%s flags=%s startId=%s", intent, flags, startId)
}

fun Logger.onBind(intent: Intent?) {
    logLifecycle("onBind() intent=%s", intent)
}

fun Logger.onUnbind(intent: Intent?) {
    logLifecycle("onUnbind() intent=%s", intent)
}

fun Logger.onRebind(intent: Intent?) {
    logLifecycle("onRebind() intent=%s", intent)
}

fun Logger.onSaveInstanceState(bundle: Bundle?) {
    logLifecycle("onSaveInstanceState() bundle=%s", bundle)
}

fun Logger.call(method: String?, arg: String?, extras: Bundle?) {
    logLifecycle("call() method=%s arg=%s extras=%s", method, arg, extras)
}

fun Logger.onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    logLifecycle("onUpgrade() db=%s oldVersion=%d newVersion=%d", db, oldVersion, newVersion)
}

fun Logger.onConfigure(db: SQLiteDatabase?) {
    logLifecycle("onConfigure() db=%s", db)
}

fun Logger.onServiceConnected(name: ComponentName?, service: IBinder?) {
    logLifecycle("onServiceConnected() name=%s service=%s", name, service)
}

fun Logger.onServiceDisconnected(name: ComponentName?) {
    logLifecycle("onServiceDisconnected() name=%s", name)
}
