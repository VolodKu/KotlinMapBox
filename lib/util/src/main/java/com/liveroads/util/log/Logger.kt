package com.liveroads.util.log

import java.util.Formatter
import java.util.IllegalFormatException
import java.util.Locale

class Logger(val name: String, val config: Config) {

    data class Config(
            val level: LogLevel,
            val logEmitters: List<LogEmitter>,
            val debugMode: Boolean
    )

    fun isLogEnabled(level: LogLevel) = (level <= config.level)

    fun v(message: String, vararg args: Any?) {
        _log(LogLevel.VERBOSE, null, null, message, args)
    }

    fun v(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.VERBOSE, exception, null, message, args)
    }

    fun d(message: String, vararg args: Any?) {
        _log(LogLevel.DEBUG, null, null, message, args)
    }

    fun d(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.DEBUG, exception, null, message, args)
    }

    fun i(message: String, vararg args: Any?) {
        _log(LogLevel.INFO, null, null, message, args)
    }

    fun i(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.INFO, exception, null, message, args)
    }

    fun w(message: String, vararg args: Any?) {
        _log(LogLevel.WARNING, null, "WARNING: ", message, args)
    }

    fun w(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.WARNING, exception, "WARNING: ", message, args)
    }

    fun e(message: String, vararg args: Any?) {
        _log(LogLevel.ERROR, null, "ERROR: ", message, args)
    }

    fun e(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.ERROR, exception, "ERROR: ", message, args)
    }

    fun log(level: LogLevel, exception: Throwable?, prefix: String?, message: String,
            vararg args: Any?) {
        _log(level, exception, prefix, message, args)
    }

    internal fun _log(level: LogLevel, exception: Throwable?, prefix: String?, message: String,
            args: Array<out Any?>) {
        if (isLogEnabled(level)) {
            val formattedMessage = formatMessage(prefix, message, args)
            for (logEmitter in config.logEmitters) {
                logEmitter.emit(level, formattedMessage, exception)
            }
        }
    }

    private fun formatMessage(prefix: String?, message: String, args: Array<out Any?>): String {
        val formatterObjects = threadLocalFormatObjects.get()
        val sb = formatterObjects.sb
        val formatter = formatterObjects.formatter

        sb.setLength(0)
        sb.append(name)
        sb.append(": ")
        if (prefix != null) {
            sb.append(prefix)
        }

        if (config.debugMode) {
            formatter.format(message, *args)
        } else {
            val lengthBefore = sb.length
            try {
                formatter.format(message, *args)
            } catch (e: IllegalFormatException) {
                sb.setLength(lengthBefore)
                sb.append(message)
                for (arg in args) {
                    sb.append(' ')
                    sb.append(arg)
                }
            }
        }

        val formattedMessage = sb.toString()

        if (sb.length < 2000) {
            sb.setLength(0)
        } else {
            threadLocalFormatObjects.remove()
        }

        return formattedMessage
    }

}

enum class LogLevel {
    ERROR,
    WARNING,
    INFO,
    DEBUG,
    VERBOSE,
}

interface LogEmitter {
    fun emit(level: LogLevel, message: String, exception: Throwable? = null)
}

private class FormatObjects {
    val sb = StringBuilder()
    val formatter = Formatter(sb, Locale.US)
}

private val threadLocalFormatObjects = object : ThreadLocal<FormatObjects>() {
    override fun initialValue() = FormatObjects()
}

