package com.liveroads.ui

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.liveroads.util.findViewByIdOrThrow

class LoginEmailPasswordFragmentView : ConstraintLayout {

    var listener: Listener? = null

    val loginButton: Button
    val createButton: Button
    val resetButton: Button
    val emailView: TextView
    val passwordView: TextView
    val errorMessageView: TextView
    val errorDetailsView: TextView
    val progressTextView: TextView
    val progressBar: ProgressBar

    constructor(context: Context)
            : super(context)

    constructor(context: Context, attrs: AttributeSet?)
            : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.lr_view_login_email_password_fragment, this)
        loginButton = findViewByIdOrThrow(R.id.login)
        createButton = findViewByIdOrThrow(R.id.create)
        resetButton = findViewByIdOrThrow(R.id.reset)
        emailView = findViewByIdOrThrow(R.id.email_edittext)
        passwordView = findViewByIdOrThrow(R.id.password_edittext)
        errorMessageView = findViewByIdOrThrow(R.id.error_message)
        errorDetailsView = findViewByIdOrThrow(R.id.error_details)
        progressTextView = findViewByIdOrThrow(R.id.progress_message)
        progressBar = findViewByIdOrThrow(R.id.progress_bar)

        ButtonClickListener().let {
            loginButton.setOnClickListener(it)
            createButton.setOnClickListener(it)
            resetButton.setOnClickListener(it)
        }

        emailView.addTextChangedListener(EmailTextWatcher())
        passwordView.addTextChangedListener(PasswordTextWatcher())
    }

    interface Listener {
        fun onLoginClick(view: LoginEmailPasswordFragmentView)
        fun onCreateClick(view: LoginEmailPasswordFragmentView)
        fun onResetClick(view: LoginEmailPasswordFragmentView)
        fun onEmailChanged(view: LoginEmailPasswordFragmentView)
        fun onPasswordChanged(view: LoginEmailPasswordFragmentView)
    }

    private inner class ButtonClickListener : OnClickListener {

        override fun onClick(view: View) {
            if (view === loginButton) {
                listener?.onLoginClick(this@LoginEmailPasswordFragmentView)
            } else if (view === createButton) {
                listener?.onCreateClick(this@LoginEmailPasswordFragmentView)
            } else if (view === resetButton) {
                listener?.onResetClick(this@LoginEmailPasswordFragmentView)
            } else {
                throw IllegalArgumentException("unknown view: $view")
            }
        }

    }

    private inner class EmailTextWatcher : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            listener?.onEmailChanged(this@LoginEmailPasswordFragmentView)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }

    private inner class PasswordTextWatcher : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            listener?.onPasswordChanged(this@LoginEmailPasswordFragmentView)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }

}
