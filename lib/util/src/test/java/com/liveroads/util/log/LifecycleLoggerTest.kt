package com.liveroads.util.log

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LifecycleLoggerTest {

    @Test
    fun test_onReceive() {
        assertLifecycleLog("onReceive() context=null intent=null") {
            onReceive(null, null)
        }
        val context = mock(Context::class.java)
        val intent = mock(Intent::class.java)
        assertLifecycleLog("onReceive() context=$context intent=$intent") {
            onReceive(context, intent)
        }
    }

    @Test
    fun test_onCreate() {
        assertLifecycleLog("onCreate()") {
            onCreate()
        }
    }

    @Test
    fun test_onCreate_Bundle() {
        assertLifecycleLog("onCreate() savedInstanceState=null") {
            onCreate(null as Bundle?)
        }
        val savedInstanceState = mock(Bundle::class.java)
        assertLifecycleLog("onCreate() savedInstanceState=$savedInstanceState") {
            onCreate(savedInstanceState)
        }
    }

    @Test
    fun test_onCreate_SQLiteDatabase() {
        assertLifecycleLog("onCreate() db=null") {
            onCreate(null as SQLiteDatabase?)
        }
        val db = mock(SQLiteDatabase::class.java)
        assertLifecycleLog("onCreate() db=$db") {
            onCreate(db)
        }
    }

    @Test
    fun test_onDestroy() {
        assertLifecycleLog("onDestroy()") { onDestroy() }
    }

    @Test
    fun test_onCreateView() {
        assertLifecycleLog("onCreateView() inflater=null container=null savedInstanceState=null") {
            onCreateView(null, null, null)
        }
        val inflater = mock(LayoutInflater::class.java)
        val container = mock(ViewGroup::class.java)
        val savedInstanceState = mock(Bundle::class.java)
        assertLifecycleLog("onCreateView() inflater=$inflater container=$container savedInstanceState=$savedInstanceState") {
            onCreateView(inflater, container, savedInstanceState)
        }
    }

    @Test
    fun test_onDestroyView() {
        assertLifecycleLog("onDestroyView()") { onDestroyView() }
    }

    @Test
    fun test_onActivityCreated() {
        assertLifecycleLog("onActivityCreated() savedInstanceState=null") {
            onActivityCreated(null as Bundle?)
        }
        val savedInstanceState = mock(Bundle::class.java)
        assertLifecycleLog("onActivityCreated() savedInstanceState=$savedInstanceState") {
            onActivityCreated(savedInstanceState)
        }
    }

    @Test
    fun test_onViewCreated() {
        assertLifecycleLog("onViewCreated() view=null savedInstanceState=null") {
            onViewCreated(null, null)
        }
        val view = mock(View::class.java)
        val savedInstanceState = mock(Bundle::class.java)
        assertLifecycleLog("onViewCreated() view=$view savedInstanceState=$savedInstanceState") {
            onViewCreated(view, savedInstanceState)
        }
    }

    @Test
    fun test_onStart() {
        assertLifecycleLog("onStart()") { onStart() }
    }

    @Test
    fun test_onStop() {
        assertLifecycleLog("onStop()") { onStop() }
    }

    @Test
    fun test_onResume() {
        assertLifecycleLog("onResume()") { onResume() }
    }

    @Test
    fun test_onPause() {
        assertLifecycleLog("onPause()") { onPause() }
    }

    @Test
    fun test_onActivityResult() {
        assertLifecycleLog("onActivityResult() requestCode=0 resultCode=0 data=null") {
            onActivityResult(0, 0, null)
        }
        val requestCode = 123
        val resultCode = 456
        val data = mock(Intent::class.java)
        assertLifecycleLog("onActivityResult() requestCode=$requestCode resultCode=$resultCode data=$data") {
            onActivityResult(requestCode, resultCode, data)
        }
    }

    @Test
    fun test_onSaveInstanceState() {
        assertLifecycleLog("onSaveInstanceState() bundle=null") {
            onSaveInstanceState(null)
        }
        val bundle = mock(Bundle::class.java)
        assertLifecycleLog("onSaveInstanceState() bundle=$bundle") {
            onSaveInstanceState(bundle)
        }
    }

    @Test
    fun test_call() {
        assertLifecycleLog("call() method=null arg=null extras=null") {
            call(null, null, null)
        }
        val method = "abc"
        val arg = "def"
        val extras = mock(Bundle::class.java)
        assertLifecycleLog("call() method=$method arg=$arg extras=$extras") {
            call(method, arg, extras)
        }
    }

    @Test
    fun test_onUpgrade() {
        assertLifecycleLog("onUpgrade() db=null oldVersion=1 newVersion=2") {
            onUpgrade(null, 1, 2)
        }
        val db = mock(SQLiteDatabase::class.java)
        assertLifecycleLog("onUpgrade() db=$db oldVersion=5 newVersion=9") {
            onUpgrade(db, 5, 9)
        }
    }

    @Test
    fun test_onConfigure() {
        assertLifecycleLog("onConfigure() db=null") {
            onConfigure(null)
        }
        val db = mock(SQLiteDatabase::class.java)
        assertLifecycleLog("onConfigure() db=$db") {
            onConfigure(db)
        }
    }

    private fun assertLifecycleLog(expectedMessage: String, block: Logger.() -> Unit) {
        val logEmitter = mock(LogEmitter::class.java)
        val logEmitters = listOf(logEmitter)
        val config = Logger.Config(LogLevel.VERBOSE, logEmitters, true)
        val logger = Logger("xNAMEx", config)
        block(logger)
        val fullExpectedMessage = "xNAMEx: $expectedMessage"
        Mockito.verify(logEmitter, Mockito.only()).emit(LogLevel.VERBOSE, fullExpectedMessage, null)
    }

}
