package com.liveroads.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liveroads.common.log.obtainLogger
import com.liveroads.ui.AnimatedButton
import com.liveroads.ui.MainScreenButtonView
import com.liveroads.util.log.*

class MainActionsFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    var listener: Listener? = null

    var isGoToMyLocationButtonVisible = true
        set(value) {
            field = value
            if (isStarted) {
                updateGoToMyLocationButtonVisibility()
            }
        }

    private val logger = obtainLogger()

    val hamburgerButton: MainScreenButtonView
        get() = view!!.findViewById(R.id.lr_fragment_main_actions_hamburger_button)
    val searchButton: MainScreenButtonView
        get() = view!!.findViewById(R.id.lr_fragment_main_actions_search_button)
    val goToMyLocationButton: MainScreenButtonView
        get() = view!!.findViewById(R.id.lr_fragment_main_actions_go_to_my_location_button)
    val liveRoadsButton: AnimatedButton
        get() = view!!.findViewById(R.id.lr_fragment_main_actions_liveroads_button)
    val estimationPanelPlaceHolder: View
        get() = view!!.findViewById(R.id.lr_estimation_panel_space_holder)

    private var isStarted = false

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
        return inflater.inflate(R.layout.lr_fragment_main_actions, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        logger.onViewCreated(view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        fixBottomSpacing()
        setHamburgerButtonDrawable()

        hamburgerButton.setOnClickListener(this)
        searchButton.setOnClickListener(this)
        goToMyLocationButton.setOnClickListener(this)
        goToMyLocationButton.setOnLongClickListener(this)
        liveRoadsButton.setOnClickListener(this)
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()
        isStarted = true
        updateGoToMyLocationButtonVisibility()
    }

    override fun onStop() {
        logger.onStop()
        isStarted = false
        super.onStop()
    }

    private fun fixBottomSpacing() {
        val estimatePanelVisibleOnLargeMap =
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        estimationPanelPlaceHolder.visibility = if (estimatePanelVisibleOnLargeMap)
            View.VISIBLE else View.GONE
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        fixBottomSpacing()
    }

    override fun onClick(view: View) {
        val action = if (view === hamburgerButton) {
            Action.TOGGLE_NAVIGATION_MENU
        } else if (view === searchButton) {
            Action.SEARCH
        } else if (view === goToMyLocationButton) {
            Action.GO_TO_MY_LOCATION
        } else if (view === liveRoadsButton) {
            Action.TOGGLE_PIP
        } else {
            throw IllegalArgumentException("unknown view: $view")
        }

        listener?.onActionSelected(this, action)
    }

    override fun onLongClick(view: View): Boolean {
        val action = if (view === goToMyLocationButton) {
            Action.GO_TO_MY_LOCATION_AND_RESET_CAMERA
        } else {
            throw IllegalArgumentException("unknown view: $view")
        }
        listener?.onActionSelected(this, action)
        return true
    }

    private fun setHamburgerButtonDrawable() {
        val drawerArrowDrawable = DrawerArrowDrawable(context)
        drawerArrowDrawable.paint.color = Color.BLACK
        hamburgerButton.imageView.setImageDrawable(drawerArrowDrawable)
    }

    private fun updateGoToMyLocationButtonVisibility() {
        val visibility = if (isGoToMyLocationButtonVisible) View.VISIBLE else View.INVISIBLE
        goToMyLocationButton.visibility = visibility
    }

    interface Listener {
        fun onActionSelected(fragment: MainActionsFragment, action: Action)
    }

    enum class Action {
        TOGGLE_NAVIGATION_MENU,
        SEARCH,
        TOGGLE_PIP,
        GO_TO_MY_LOCATION,
        GO_TO_MY_LOCATION_AND_RESET_CAMERA,
    }

}
