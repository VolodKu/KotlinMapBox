package com.liveroads.devtools

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.liveroads.common.devtools.devTools
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.main.R
import com.liveroads.devtools.main.R.id.lr_devtools_reset_to_defaults
import com.liveroads.util.findFragmentByIdOrReplace
import com.liveroads.util.log.onCreate
import com.liveroads.util.log.onDestroy

class DevToolsActivity : AppCompatActivity() {

    private val logger = obtainLogger()

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lr_activity_devtools)

        val toolbar = findViewById<Toolbar>(R.id.lr_activity_devtools_toolbar)
        setSupportActionBar(toolbar)

        val showUpInToolbar = intent?.getBooleanExtra(EXTRA_SHOW_BACK_IN_TOOLBAR, false) ?: false
        if (showUpInToolbar) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager.findFragmentByIdOrReplace(R.id.lr_activity_devtools_content) {
            DevToolsFragment()
        }
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        MenuInflater(this).apply {
            inflate(R.menu.lr_activity_devtools, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            lr_devtools_reset_to_defaults -> {
                devTools.clear()
            }
            android.R.id.home -> {
                finish()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    companion object {

        const val EXTRA_SHOW_BACK_IN_TOOLBAR = "liveroads.DevToolsActivity.EXTRA_SHOW_BACK_IN_TOOLBAR"

        fun start(context: Context, showBackInToolbar: Boolean) {
            val intent = Intent(context, DevToolsActivity::class.java)
            intent.putExtra(EXTRA_SHOW_BACK_IN_TOOLBAR, showBackInToolbar)
            context.startActivity(intent)
        }

    }

}
