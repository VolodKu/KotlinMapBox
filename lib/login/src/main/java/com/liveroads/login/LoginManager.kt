package com.liveroads.login

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.annotation.AnyThread
import android.support.annotation.BinderThread
import android.support.annotation.MainThread
import android.support.v4.util.SimpleArrayMap
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.liveroads.common.log.obtainLogger
import com.liveroads.util.startServiceOrThrow
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class LoginManager : ILoginService.Stub() {

    private val tasks = SimpleArrayMap<IBinder, LoginTask>()
    private val tasksLock = ReentrantReadWriteLock()
    private val listenersLock = ReentrantReadWriteLock()

    private lateinit var appContext: Context
    private lateinit var listeners: MutableList<ILoginServiceListener>
    private lateinit var firebaseAuth: FirebaseAuth

    @MainThread
    fun onCreate(context: Context) {
        appContext = context.applicationContext
        listeners = mutableListOf()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    @MainThread
    fun onDestroy() {
        tasksLock.write { tasks.clear() }
        listenersLock.write { listeners.clear() }
    }

    @MainThread
    fun createEmailPasswordAccount(id: IBinder, email: String, password: String) {
        val task = CreateEmailPasswordAccountTask(appContext, this, id, email, password)
        tasksLock.write { tasks.put(id, task) }
        task.start(firebaseAuth)
    }

    @MainThread
    fun loginEmailPassword(id: IBinder, email: String, password: String) {
        val task = LoginEmailPasswordTask(appContext, this, id, email, password)
        tasksLock.write { tasks.put(id, task) }
        task.start(firebaseAuth)
    }

    @MainThread
    fun cancelTask(id: IBinder) {
        tasksLock.read { tasks.get(id) }?.cancel()
    }

    @AnyThread
    fun notifyTaskComplete(id: IBinder) {
        listenersLock.read {
            for (curListener in listeners) {
                try {
                    curListener.onTaskStateChanged(id)
                } catch (e: RemoteException) {
                }
            }
        }
    }

    @BinderThread
    override fun addListener(listener: ILoginServiceListener?) {
        if (listener == null) {
            throw NullPointerException("listener==null")
        }
        listenersLock.write {
            listeners.add(listener)
        }
    }

    @BinderThread
    override fun removeListener(listener: ILoginServiceListener?) {
        if (listener != null) {
            listenersLock.write {
                for (i in 0..listeners.size - 1) {
                    if (listeners[i].asBinder() == listener.asBinder()) {
                        listeners.removeAt(i)
                        break
                    }
                }
            }
        }
    }

    @BinderThread
    override fun isTaskComplete(id: IBinder?): Boolean {
        if (id == null) {
            return false
        }
        val task = tasksLock.read { tasks.get(id) }
        return (task != null && task.complete)
    }

    @BinderThread
    override fun getTaskResult(id: IBinder?): LoginTaskResult? {
        if (id == null) {
            return null
        }
        val task = tasksLock.read { tasks.get(id) }
        if (task == null || !task.complete) {
            return null
        }
        return LoginTaskResult(task.error, task.errorMessage)
    }

}

private sealed class LoginTask(val context: Context, val loginManager: LoginManager, val id: IBinder) {

    @Volatile
    var complete = false
        private set

    var error: LoginTaskResult.Error? = null
    var errorMessage: String? = null

    abstract fun start(auth: FirebaseAuth)

    @MainThread
    open fun cancel() {
    }

    protected fun notifyComplete() {
        complete = true

        loginManager.notifyTaskComplete(id)

        Intent().let { intent ->
            intent.setClass(context, LoginService::class.java)
            intent.action = LoginService.ACTION_TASK_COMPLETE
            intent.replaceExtras(Bundle().apply {
                putBinder(LoginService.EXTRA_ID, id)
            })
            context.startServiceOrThrow(intent)
        }

    }

}

private class CreateEmailPasswordAccountTask(
        context: Context,
        loginManager: LoginManager,
        id: IBinder,
        val email: String,
        val password: String)
    : LoginTask(context, loginManager, id), OnCompleteListener<AuthResult> {

    private val logger = obtainLogger(this)

    override fun start(auth: FirebaseAuth) {
        logger.i("Creating account with email: %s", email)

        val task = try {
            auth.createUserWithEmailAndPassword(email, password)
        } catch (e: FirebaseException) {
            handleAuthException(e)
            notifyComplete()
            return
        }

        task.addOnCompleteListener(AsyncTask.THREAD_POOL_EXECUTOR, this)
    }

    override fun onComplete(task: Task<AuthResult>) {
        if (task.isSuccessful) {
            logger.i("Creating account with email completed successfully: %s (%s)", email, task.result?.user)
        } else {
            handleAuthException(task.exception)
        }
        notifyComplete()
    }

    private fun handleAuthException(e: Exception?) {
        error = when (e) {
            is FirebaseAuthWeakPasswordException -> LoginTaskResult.Error.WEAK_PASSWORD
            is FirebaseAuthInvalidCredentialsException -> LoginTaskResult.Error.MALFORMED_EMAIL
            is FirebaseAuthUserCollisionException -> LoginTaskResult.Error.ALREADY_EXISTS
            is FirebaseNetworkException -> LoginTaskResult.Error.NETWORK_ERROR
            else -> LoginTaskResult.Error.UNKNOWN
        }
        errorMessage = e.toString()
        logger.w("Creating account with email %s failed: %s %s", email, error, errorMessage)
    }

}

private class LoginEmailPasswordTask(
        context: Context,
        loginManager: LoginManager,
        id: IBinder,
        val email: String,
        val password: String)
    : LoginTask(context, loginManager, id), OnCompleteListener<AuthResult> {

    private val logger = obtainLogger(this)

    override fun start(auth: FirebaseAuth) {
        logger.i("Logging into account with email: %s", email)

        val task = try {
            auth.signInWithEmailAndPassword(email, password)
        } catch (e: FirebaseException) {
            handleAuthException(e)
            notifyComplete()
            return
        }

        task.addOnCompleteListener(AsyncTask.THREAD_POOL_EXECUTOR, this)
    }

    override fun onComplete(task: Task<AuthResult>) {
        if (task.isSuccessful) {
            logger.i("Logging in with account completed successfully: %s (%s)", email, task.result?.user)
        } else {
            handleAuthException(task.exception)
        }
        notifyComplete()
    }

    private fun handleAuthException(e: Exception?) {
        error = when (e) {
            is FirebaseAuthInvalidUserException -> LoginTaskResult.Error.INVALID_USER
            is FirebaseAuthInvalidCredentialsException -> LoginTaskResult.Error.INVALID_PASSWORD
            is FirebaseNetworkException -> LoginTaskResult.Error.NETWORK_ERROR
            else -> LoginTaskResult.Error.UNKNOWN
        }
        errorMessage = e.toString()
        logger.w("Logging in with email %s failed: %s %s", email, error, errorMessage)
    }

}
