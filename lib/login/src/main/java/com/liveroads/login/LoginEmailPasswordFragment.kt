package com.liveroads.login

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.RemoteException
import android.support.annotation.BinderThread
import android.support.annotation.MainThread
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liveroads.common.log.obtainLogger
import com.liveroads.ui.LoginEmailPasswordFragmentView
import com.liveroads.util.bindServiceOrThrow
import com.liveroads.util.log.*
import com.liveroads.util.startServiceOrThrow
import java.lang.ref.WeakReference

private const val KEY_TASK_ID = "LiveRoads.LoginEmailPasswordFragment.KEY_TASK_ID"
private const val KEY_TASK_ACTION = "LiveRoads.LoginEmailPasswordFragment.KEY_TASK_ACTION"
private const val KEY_TASK_ERROR = "LiveRoads.LoginEmailPasswordFragment.KEY_TASK_ERROR"

class LoginEmailPasswordFragment : Fragment(), ServiceConnection, LoginEmailPasswordFragmentView.Listener {

    private val logger = obtainLogger(this)

    private lateinit var selfRef: WeakReference<LoginEmailPasswordFragment>
    private lateinit var mainHandler: MainHandler
    private lateinit var loginServiceListener: LoginServiceListener

    private var loginService: ILoginService? = null
    private var taskId: IBinder? = null
    private var taskAction: Action? = null
    private var taskError: LoginTaskResult.Error? = null

    private val view: LoginEmailPasswordFragmentView
        get() = super.getView() as LoginEmailPasswordFragmentView

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        selfRef = WeakReference(this)
        mainHandler = MainHandler(selfRef)
        loginServiceListener = LoginServiceListener(mainHandler)

        savedInstanceState?.apply {
            taskId = getBinder(KEY_TASK_ID)
            taskAction = getString(KEY_TASK_ACTION)?.let { Action.valueOf(it) }
            taskError = getString(KEY_TASK_ERROR)?.let { LoginTaskResult.Error.valueOf(it) }
        }

        Intent().let { intent ->
            intent.setClass(context, LoginService::class.java)
            context.bindServiceOrThrow(intent, this)
        }
    }

    override fun onDestroy() {
        logger.onDestroy()

        try {
            loginService?.removeListener(loginServiceListener)
        } catch (ignored: RemoteException) {
        }

        context.unbindService(this)
        loginService = null

        mainHandler.removeCallbacksAndMessages(null)
        selfRef.clear()

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_login_email_password, container, false)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
        view.listener = this
        updateUi()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putBinder(KEY_TASK_ID, taskId)
        bundle.putString(KEY_TASK_ACTION, taskAction?.name)
        bundle.putString(KEY_TASK_ERROR, taskError?.name)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        logger.onServiceDisconnected(name)

        try {
            loginService?.removeListener(loginServiceListener)
        } catch (ignored: RemoteException) {
        }

        loginService = null
        updateUi()
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        logger.onServiceConnected(name, binder)
        loginService = ILoginService.Stub.asInterface(binder)

        try {
            loginService!!.addListener(loginServiceListener)
        } catch (ignored: RemoteException) {
        }

        taskId?.let {
            handleTaskStateChanged(it)
        }

        updateUi()
    }

    override fun onLoginClick(view: LoginEmailPasswordFragmentView) {
        logger.logLifecycle("onLoginClick()")
        performUsernamePasswordAction(LoginService.ACTION_LOGIN_EMAIL_PASSWORD, Action.LOGIN)
    }

    override fun onCreateClick(view: LoginEmailPasswordFragmentView) {
        logger.logLifecycle("onCreateClick()")
        performUsernamePasswordAction(LoginService.ACTION_CREATE_ACCOUNT_EMAIL_PASSWORD, Action.CREATE_ACCOUNT)
    }

    private fun performUsernamePasswordAction(intentAction: String, action: Action) {
        val email = view.emailView.text?.toString()?.trim()
        val password = view.passwordView.text?.toString()?.trim()

        if (email == null || password == null) {
            return
        }

        val taskId = Binder()

        Intent().let { intent ->
            intent.setClass(context, LoginService::class.java)
            intent.action = intentAction
            intent.replaceExtras(Bundle().apply {
                putBinder(LoginService.EXTRA_ID, taskId)
                putString(LoginService.EXTRA_EMAIL, email)
                putString(LoginService.EXTRA_PASSWORD, password)
            })
            context.startServiceOrThrow(intent)
        }

        this.taskId = taskId
        this.taskAction = action

        updateUi()
    }

    override fun onResetClick(view: LoginEmailPasswordFragmentView) {
        logger.logLifecycle("onResetClick()")
    }

    override fun onEmailChanged(view: LoginEmailPasswordFragmentView) {
        updateUi()
    }

    override fun onPasswordChanged(view: LoginEmailPasswordFragmentView) {
        updateUi()
    }

    private fun updateUi() {
        val email = view.emailView.text
        val password = view.passwordView.text

        val emailValid = isValidEmail(email)
        val passwordValid = isValidPassword(password)

        view.loginButton.isEnabled = (taskId == null && emailValid && passwordValid)
        view.createButton.isEnabled = (taskId == null && emailValid && passwordValid)
        view.resetButton.isEnabled = (taskId == null && emailValid)
        view.emailView.isEnabled = (taskId == null)
        view.passwordView.isEnabled = (taskId == null)

        view.progressTextView.setText(when (taskAction) {
            Action.LOGIN -> R.string.lr_msg_logging_in
            Action.CREATE_ACCOUNT -> R.string.lr_msg_creating_account
            else -> R.string.empty
        })

        view.errorMessageView.setText(when (taskAction) {
            Action.LOGIN -> R.string.lr_error_login
            Action.CREATE_ACCOUNT -> R.string.lr_error_create_account
            else -> R.string.empty
        })

        view.errorDetailsView.setText(when (taskAction) {
            Action.LOGIN -> when (taskError) {
                LoginTaskResult.Error.INVALID_USER -> R.string.lr_error_login_unknown_user
                LoginTaskResult.Error.INVALID_PASSWORD -> R.string.lr_error_login_invalid_password
                LoginTaskResult.Error.NETWORK_ERROR -> R.string.lr_error_network_error
                else -> R.string.lr_error_login_unknown
            }
            Action.CREATE_ACCOUNT -> when (taskError) {
                LoginTaskResult.Error.WEAK_PASSWORD -> R.string.lr_error_create_account_weak_password
                LoginTaskResult.Error.MALFORMED_EMAIL -> R.string.lr_error_create_account_malformed_email
                LoginTaskResult.Error.ALREADY_EXISTS -> R.string.lr_error_create_account_already_exists
                LoginTaskResult.Error.NETWORK_ERROR -> R.string.lr_error_network_error
                else -> R.string.lr_error_create_account_unknown
            }
            else -> R.string.empty
        })

        view.errorMessageView.visibility = if (taskId != null || taskError == null) View.GONE else View.VISIBLE
        view.errorDetailsView.visibility = if (taskId != null || taskError == null) View.GONE else View.VISIBLE
        view.progressBar.visibility = if (taskId == null) View.GONE else View.VISIBLE
        view.progressTextView.visibility = if (taskId == null) View.GONE else View.VISIBLE
        view.loginButton.visibility = if (taskId != null) View.GONE else View.VISIBLE
        view.createButton.visibility = if (taskId != null) View.GONE else View.VISIBLE
        view.resetButton.visibility = View.GONE // TODO: implement reset button
    }

    private fun handleTaskStateChanged(id: IBinder) {
        val taskIsComplete = try {
            id === taskId && loginService?.isTaskComplete(id) ?: false
        } catch (ignored: RemoteException) {
            false
        }

        if (!taskIsComplete) {
            return
        }

        val result = try {
            loginService?.getTaskResult(id)
        } catch (e: RemoteException) {
            null
        }

        if (result == null) {
            return
        }

        logger.i("Login task complete: error=%s", result.error)
        taskId = null
        taskError = result.error

        if (result.error == null) {
            (activity as LoginActivity?)?.onLoginSuccess()
        }

        updateUi()
    }

    private class LoginServiceListener(val mainHandler: MainHandler) : ILoginServiceListener.Stub() {

        @BinderThread
        override fun onTaskStateChanged(id: IBinder?) {
            if (id != null) {
                mainHandler.obtainMessage(MainHandler.MSG_TASK_STATE_CHANGED, id).sendToTarget()
            }
        }

    }

    @MainThread
    private class MainHandler(val fragment: WeakReference<LoginEmailPasswordFragment>) : Handler() {

        override fun handleMessage(message: Message) {
            when (message.what) {
                MSG_TASK_STATE_CHANGED -> {
                    fragment.get()?.handleTaskStateChanged(message.obj as IBinder)
                }
                else -> throw IllegalArgumentException("unknown message: ${message.what}")
            }
        }

        companion object {
            const val MSG_TASK_STATE_CHANGED = 1
        }

    }

    private enum class Action {
        LOGIN,
        CREATE_ACCOUNT,
    }

}
