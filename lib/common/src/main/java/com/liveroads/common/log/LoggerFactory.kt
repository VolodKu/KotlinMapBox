package com.liveroads.common.log

import android.util.LruCache
import com.liveroads.util.log.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

private val loggerPool = LoggerPool()

fun init(mode: LogMode) {
    val logLevel: LogLevel
    val debugMode: Boolean
    when (mode) {
        LogMode.DEBUG -> {
            logLevel = LogLevel.VERBOSE
            debugMode = true
        }
        LogMode.RELEASE -> {
            logLevel = LogLevel.INFO
            debugMode = false
        }
    }

    loggerPool.config = Logger.Config(
            level = logLevel,
            logEmitters = listOf(LogcatLogEmitter("LiveRoads")),
            debugMode = debugMode
    )
}

fun obtainLogger(obj: Any): Logger {
    var name = obj::class.java.name
    if (name == null) {
        name = "Object@${System.identityHashCode(obj)}"
    } else {
        val index = name.lastIndexOf('.')
        if (index >= 0 && index < name.length - 1) {
            name = name.substring(index + 1)
        }
    }
    return obtainLogger(name)
}

fun obtainLogger(name: String): Logger {
    return loggerPool.obtain(name)
}

enum class LogMode {
    DEBUG,
    RELEASE,
}

private class LoggerPool {

    @Volatile
    var config: Logger.Config? = null

    private val loggers = LruCache<String, Logger>(20)
    private val readLock: Lock
    private val writeLock: Lock

    init {
        val lock = ReentrantReadWriteLock()
        readLock = lock.readLock()
        writeLock = lock.writeLock()
    }

    fun obtain(name: String): Logger {
        readLock.lock()
        val cachedLogger = try {
            loggers.get(name)
        } finally {
            readLock.unlock()
        }

        if (cachedLogger != null) {
            return cachedLogger
        }

        writeLock.lock()
        return try {
            val logger = Logger(name, config!!)
            loggers.put(name, logger)
            logger
        } finally {
            writeLock.unlock()
        }
    }

}
