package com.liveroads.login

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.liveroads.util.findFragmentByIdOrReplace

class LoginActivity : AppCompatActivity() {

    private lateinit var nextIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nextIntent = intent!!.getParcelableExtra(EXTRA_NEXT_INTENT)

        setContentView(R.layout.lr_activity_login)

        supportFragmentManager.findFragmentByIdOrReplace(R.id.lr_activity_login_container) {
            LoginEmailPasswordFragment()
        }
    }

    fun onLoginSuccess() {
        startActivity(nextIntent)
        finish()
    }

    companion object {
        const val EXTRA_NEXT_INTENT = "liveroads.LoginActivity.EXTRA_NEXT_INTENT"
    }

}
