package com.liveroads.util.log

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.mockito.junit.MockitoJUnitRunner::class)
class LoggerTest {

    @Test
    fun test_isLogEnabled() {
        for (loggerLogLevel in LogLevel.values()) {
            for (logLevel in LogLevel.values()) {
                val config = Logger.Config(loggerLogLevel, emptyList<LogEmitter>(), false)
                val logger = Logger("abcd", config)
                val actual = logger.isLogEnabled(logLevel)
                val expected = shouldLogLevelBeEnabled(logLevel, loggerLogLevel)
                kotlin.test.assertEquals(expected, actual, "loggerLogLevel=$loggerLogLevel logLevel=$logLevel")
            }
        }
    }

    @Test
    fun test_v() {
        test_log_function(Logger::v, LogLevel.VERBOSE, "")
        test_log_function_exception(Logger::v, LogLevel.VERBOSE, "")
    }

    @Test
    fun test_d() {
        test_log_function(Logger::d, LogLevel.DEBUG, "")
        test_log_function_exception(Logger::d, LogLevel.DEBUG, "")
    }

    @Test
    fun test_i() {
        test_log_function(Logger::i, LogLevel.INFO, "")
        test_log_function_exception(Logger::i, LogLevel.INFO, "")
    }

    @Test
    fun test_w() {
        val prefix = "WARNING: "
        test_log_function(Logger::w, LogLevel.WARNING, prefix)
        test_log_function_exception(Logger::w, LogLevel.WARNING, prefix)
    }

    @Test
    fun test_e() {
        val prefix = "ERROR: "
        test_log_function(Logger::e, LogLevel.ERROR, prefix)
        test_log_function_exception(Logger::e, LogLevel.ERROR, prefix)
    }

}

private fun test_log_function(func: (Logger, String, Array<out Any?>) -> Unit,
        funcLevel: LogLevel, prefix: String) {
    for (loggerLogLevel in LogLevel.values()) {
        for (debugMode in listOf(true, false)) {
            test_log_function(func, funcLevel, loggerLogLevel, debugMode, prefix)
        }
    }
}

private fun test_log_function(func: (Logger, String, Array<out Any?>) -> Unit,
        funcLevel: LogLevel, loggerLogLevel: LogLevel, debugMode: Boolean, prefix: String) {
    val logEmitter = org.mockito.Mockito.mock(LogEmitter::class.java)
    val logEmitters = listOf(logEmitter)
    val config = Logger.Config(loggerLogLevel, logEmitters, debugMode)
    val logger = Logger("xNAMEx", config)
    val shouldEmit = shouldLogLevelBeEnabled(funcLevel, loggerLogLevel)

    // NOTE: the code below is (essentially) duplicated test_log_function_exception();
    // be sure that any changes to this code block are copied to the other, and vice versa
    func(logger, "hello", arrayOf())
    if (shouldEmit) {
        val expectedMessage = "xNAMEx: ${prefix}hello"
        org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, null)
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)

    func(logger, "hello %s abc %d", arrayOf("def", 123))
    if (shouldEmit) {
        val expectedMessage = "xNAMEx: ${prefix}hello def abc 123"
        org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, null)
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)

    if (shouldEmit) {
        if (debugMode) {
            kotlin.test.assertFailsWith(java.util.MissingFormatArgumentException::class) {
                func(logger, "hello %s %s", arrayOf("abc"))
            }
        } else {
            func(logger, "hello %s %s", arrayOf("abc"))
            val expectedMessage = "xNAMEx: ${prefix}hello %s %s abc"
            org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, null)
        }
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)
}

private fun test_log_function_exception(func: (Logger, Throwable?, String, Array<out Any?>) -> Unit,
        funcLevel: LogLevel, prefix: String) {
    for (loggerLogLevel in LogLevel.values()) {
        for (debugMode in listOf(true, false)) {
            test_log_function_exception(func, funcLevel, loggerLogLevel, debugMode, prefix)
        }
    }
}

private fun test_log_function_exception(func: (Logger, Throwable?, String, Array<out Any?>) -> Unit,
        funcLevel: LogLevel, loggerLogLevel: LogLevel, debugMode: Boolean, prefix: String) {
    val logEmitter = org.mockito.Mockito.mock(LogEmitter::class.java)
    val logEmitters = listOf(logEmitter)
    val config = Logger.Config(loggerLogLevel, logEmitters, debugMode)
    val logger = Logger("xNAMEx", config)
    val shouldEmit = shouldLogLevelBeEnabled(funcLevel, loggerLogLevel)

    // NOTE: the code below is adapter from code in test_log_function();
    // be sure that any changes to this code block are copied to the other, and vice versa
    func(logger, null, "hello", arrayOf())
    if (shouldEmit) {
        val expectedMessage = "xNAMEx: ${prefix}hello"
        org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, null)
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)

    func(logger, null, "hello %s abc %d", arrayOf("def", 123))
    if (shouldEmit) {
        val expectedMessage = "xNAMEx: ${prefix}hello def abc 123"
        org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, null)
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)

    if (shouldEmit) {
        if (debugMode) {
            kotlin.test.assertFailsWith(java.util.MissingFormatArgumentException::class) {
                func(logger, null, "hello %s %s", arrayOf("abc"))
            }
        } else {
            func(logger, null, "hello %s %s", arrayOf("abc"))
            val expectedMessage = "xNAMEx: ${prefix}hello %s %s abc"
            org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, null)
        }
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)

    // NOTE: the code below is copied from above, but modified to specify a non-null exception;
    // be sure that any changes to this code block are copied to the other, and vice versa
    val e = Throwable()
    func(logger, e, "hello", arrayOf())
    if (shouldEmit) {
        val expectedMessage = "xNAMEx: ${prefix}hello"
        org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, e)
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)

    func(logger, e, "hello %s abc %d", arrayOf("def", 123))
    if (shouldEmit) {
        val expectedMessage = "xNAMEx: ${prefix}hello def abc 123"
        org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, e)
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)

    if (shouldEmit) {
        if (debugMode) {
            kotlin.test.assertFailsWith(java.util.MissingFormatArgumentException::class) {
                func(logger, e, "hello %s %s", arrayOf("abc"))
            }
        } else {
            func(logger, e, "hello %s %s", arrayOf("abc"))
            val expectedMessage = "xNAMEx: ${prefix}hello %s %s abc"
            org.mockito.Mockito.verify(logEmitter, org.mockito.Mockito.only()).emit(funcLevel, expectedMessage, e)
        }
    } else {
        org.mockito.Mockito.verifyNoMoreInteractions(logEmitter)
    }
    org.mockito.Mockito.clearInvocations(logEmitter)
}

private fun shouldLogLevelBeEnabled(logLevel: LogLevel, loggerLogLevel: LogLevel): Boolean {
    return when (logLevel) {
        LogLevel.VERBOSE -> when (loggerLogLevel) {
            LogLevel.VERBOSE -> true
            LogLevel.DEBUG -> false
            LogLevel.INFO -> false
            LogLevel.WARNING -> false
            LogLevel.ERROR -> false
        }
        LogLevel.DEBUG -> when (loggerLogLevel) {
            LogLevel.VERBOSE -> true
            LogLevel.DEBUG -> true
            LogLevel.INFO -> false
            LogLevel.WARNING -> false
            LogLevel.ERROR -> false
        }
        LogLevel.INFO -> when (loggerLogLevel) {
            LogLevel.VERBOSE -> true
            LogLevel.DEBUG -> true
            LogLevel.INFO -> true
            LogLevel.WARNING -> false
            LogLevel.ERROR -> false
        }
        LogLevel.WARNING -> when (loggerLogLevel) {
            LogLevel.VERBOSE -> true
            LogLevel.DEBUG -> true
            LogLevel.INFO -> true
            LogLevel.WARNING -> true
            LogLevel.ERROR -> false
        }
        LogLevel.ERROR -> when (loggerLogLevel) {
            LogLevel.VERBOSE -> true
            LogLevel.DEBUG -> true
            LogLevel.INFO -> true
            LogLevel.WARNING -> true
            LogLevel.ERROR -> true
        }
    }
}
