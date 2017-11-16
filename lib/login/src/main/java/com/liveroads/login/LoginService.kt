package com.liveroads.login

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.util.ArraySet
import com.liveroads.common.log.obtainLogger
import com.liveroads.util.log.*

class LoginService : Service() {

    private val logger = obtainLogger(this)
    private val loginManager = LoginManager()

    private lateinit var taskIds: ArraySet<IBinder>

    override fun onCreate() {
        logger.onCreate()
        super.onCreate()
        loginManager.onCreate(this)
        taskIds = ArraySet()
    }

    override fun onDestroy() {
        logger.onDestroy()
        taskIds.clear()
        loginManager.onDestroy()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_CREATE_ACCOUNT_EMAIL_PASSWORD -> {
                val id = intent.extras.getBinder(EXTRA_ID)
                val email = intent.getStringExtra(EXTRA_EMAIL)
                val password = intent.getStringExtra(EXTRA_PASSWORD)
                if (id == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: id==null")
                } else if (email == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: email==null")
                } else if (password == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: password==null")
                } else {
                    taskIds.add(id)
                    loginManager.createEmailPasswordAccount(id, email, password)
                }
            }
            ACTION_LOGIN_EMAIL_PASSWORD -> {
                val id = intent.extras.getBinder(EXTRA_ID)
                val email = intent.getStringExtra(EXTRA_EMAIL)
                val password = intent.getStringExtra(EXTRA_PASSWORD)
                if (id == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: id==null")
                } else if (email == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: email==null")
                } else if (password == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: password==null")
                } else {
                    taskIds.add(id)
                    loginManager.loginEmailPassword(id, email, password)
                }
            }
            ACTION_CANCEL_TASK -> {
                val id = intent.extras.getBinder(EXTRA_ID)
                if (id == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: id==null")
                } else {
                    taskIds.remove(id)
                    loginManager.cancelTask(id)
                }
            }
            ACTION_TASK_COMPLETE -> {
                val id = intent.extras.getBinder(EXTRA_ID)
                if (id == null) {
                    logger.w("onStartCommand(): invalid intent=$intent: id==null")
                } else {
                    taskIds.remove(id)
                }
            }
            else -> {
                logger.w("onStartCommand(): ignoring unsupported Intent: $intent")
            }
        }

        if (taskIds.isEmpty()) {
            val result = stopSelfResult(startId)
            logger.v("stopSelfResult(startId=$startId) returned $result")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        logger.onBind(intent)
        return loginManager
    }

    override fun onUnbind(intent: Intent?): Boolean {
        logger.onUnbind(intent)
        return true
    }

    override fun onRebind(intent: Intent?) {
        logger.onRebind(intent)
        super.onRebind(intent)
    }

    companion object {
        const val ACTION_CREATE_ACCOUNT_EMAIL_PASSWORD = "liveroads.LoginService.ACTION_CREATE_ACCOUNT_EMAIL_PASSWORD"
        const val ACTION_LOGIN_EMAIL_PASSWORD = "liveroads.LoginService.ACTION_LOGIN_EMAIL_PASSWORD"
        const val ACTION_CANCEL_TASK = "liveroads.LoginService.ACTION_CANCEL_TASK"
        internal const val ACTION_TASK_COMPLETE = "liveroads.LoginService.ACTION_TASK_COMPLETE"
        const val EXTRA_ID = "liveroads.LoginService.EXTRA_ID"
        const val EXTRA_EMAIL = "liveroads.LoginService.EXTRA_EMAIL"
        const val EXTRA_PASSWORD = "liveroads.LoginService.EXTRA_PASSWORD"
    }

}
