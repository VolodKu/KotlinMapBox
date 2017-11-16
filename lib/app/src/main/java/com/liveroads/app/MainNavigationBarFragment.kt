package com.liveroads.app

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.liveroads.common.devtools.devTools
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.client.monitor.DevToolsEnabledMonitor
import com.liveroads.util.log.*

class MainNavigationBarFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {

    var listener: Listener? = null

    private val logger = obtainLogger()

    private val navigationView: NavigationView
        get() = view as NavigationView

    val devToolsEnabledMonitor = object : DevToolsEnabledMonitor(devTools) {
        override fun onValueChanged() {
            updateMenuItems()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_main_navigation_bar, container, false)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()
        devToolsEnabledMonitor.start()
        updateMenuItems()
    }

    override fun onStop() {
        logger.onStop()
        devToolsEnabledMonitor.stop()
        super.onStop()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val action = when (item.itemId) {
            R.id.logout -> Action.LOGOUT
            R.id.devtools -> Action.DEVTOOLS
            else -> null
        }

        return if (action == null) {
            false
        } else {
            listener?.onActionSelected(this, action)
            true
        }
    }

    private fun updateMenuItems() {
        navigationView.menu.findItem(R.id.devtools).isVisible = devTools.isEnabled
    }

    interface Listener {
        fun onActionSelected(fragment: MainNavigationBarFragment, action: Action)
    }

    enum class Action {
        LOGOUT,
        DEVTOOLS,
    }

}
