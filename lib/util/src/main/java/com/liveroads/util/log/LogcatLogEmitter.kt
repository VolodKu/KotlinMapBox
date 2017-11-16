package com.liveroads.util.log

class LogcatLogEmitter(val tag: String) : LogEmitter {

    override fun emit(level: LogLevel, message: String, exception: Throwable?) {
        when (level) {
            LogLevel.ERROR -> android.util.Log.e(tag, message, exception)
            LogLevel.WARNING -> android.util.Log.w(tag, message, exception)
            LogLevel.INFO -> android.util.Log.i(tag, message, exception)
            LogLevel.DEBUG -> android.util.Log.d(tag, message, exception)
            LogLevel.VERBOSE -> android.util.Log.v(tag, message, exception)
        }
    }

}
