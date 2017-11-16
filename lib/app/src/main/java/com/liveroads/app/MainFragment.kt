package com.liveroads.app

import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.widget.DrawerLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.liveroads.app.LiveRoadsMapFragment.MyLocationScreenPosition
import com.liveroads.app.search.SearchFragment
import com.liveroads.app.search.SearchFragment.SearchResult
import com.liveroads.app.search.SearchWorkFragment
import com.liveroads.common.devtools.devTools
import com.liveroads.common.log.obtainLogger
import com.liveroads.db.NavigationHistory
import com.liveroads.db.NavigationItem
import com.liveroads.devtools.client.monitor.MapGridLinesEnabledMonitor
import com.liveroads.location.LocationProviderFragment
import com.liveroads.mapzen.MapRouter
import com.liveroads.ui.GridLinesView
import com.liveroads.util.log.*
import com.mapzen.model.ValhallaLocation
import com.mapzen.valhalla.Route
import com.mapzen.valhalla.RouteCallback

class MainFragment : Fragment(),
        MainNavigationBarFragment.Listener,
        MainActionsFragment.Listener,
        MainMapsFragment.Listener,
        SearchFragment.Listener,
        LocationProviderFragment.Listener {

    var listener: Listener? = null
    lateinit var locationProviderFragment: LocationProviderFragment
    lateinit var searchWorkFragment: SearchWorkFragment

    private val logger = obtainLogger()

    private val drawerLayout: DrawerLayout
        get() = view as DrawerLayout
    private val mainNavigationBarFragment: MainNavigationBarFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_main_navigation_bar_fragment)
                as MainNavigationBarFragment
    private val mainActionsFragment: MainActionsFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_main_actions_fragment)
                as MainActionsFragment
    val mainMapsFragment: MainMapsFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_main_map_fragment)
                as MainMapsFragment
    private val searchFragment: SearchFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_main_search_fragment)
                as SearchFragment
    private val gridLinesView: GridLinesView
        get() = view!!.findViewById<GridLinesView>(R.id.lr_fragment_main_grid_lines)

    private val mapGridLinesEnabledMonitor = object : MapGridLinesEnabledMonitor(devTools) {
        override fun onValueChanged() {
            updateMapGridLinesVisibility()
        }
    }

    private var speed = Float.NaN
    private var selectedSearchResultMarkerInfo: SearchResultMarkerInfo? = null

    private val isSearchVisible: Boolean
        get() = !searchFragment.isHidden

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDb = FirebaseDatabase.getInstance()

        instanceObj = this

        navHistory = NavigationHistory(this.context, firebaseDb, firebaseAuth.currentUser?.uid)
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        logger.onViewCreated(view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            setSearchVisible(false, false)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
        mainMapsFragment.locationProviderFragment = locationProviderFragment
        searchFragment.workFragment = searchWorkFragment

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SEARCH_RESULT_MARKER_LATITUDE)) {
            val latitude = savedInstanceState.getDouble(KEY_SEARCH_RESULT_MARKER_LATITUDE, 0.0)
            val longitude = savedInstanceState.getDouble(KEY_SEARCH_RESULT_MARKER_LONGITUDE, 0.0)
            addSelectedSearchResultMarker(latitude, longitude)
        }
    }


    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
        mainNavigationBarFragment.listener = this
        mainActionsFragment.listener = this
        mainMapsFragment.listener = this
        onFollowMeEnabledChanged(mainMapsFragment)

        if (savedInstanceState != null) {
            mainActionsFragment.liveRoadsButton.let {
                it.visibility = savedInstanceState.getInt(KEY_LIVE_ROADS_BUTTON_VISIBILITY, it.visibility)
            }
            speed = savedInstanceState.getFloat(KEY_SPEED, speed)
        }
    }

    override fun onDestroyView() {
        logger.onDestroyView()
        if (mainActionsFragment.listener === this) {
            mainActionsFragment.listener = null
        }
        if (mainNavigationBarFragment.listener === this) {
            mainNavigationBarFragment.listener = null
        }
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        mapGridLinesEnabledMonitor.start()
        updateSpeed()
        updateMapGridLinesVisibility()
        updateMyLocationScreenPosition()
        locationProviderFragment.addListener(this)
    }

    override fun onStop() {
        locationProviderFragment.removeListener(this)
        mapGridLinesEnabledMonitor.stop()
        super.onStop()
    }

    override fun onResume() {
        logger.onResume()
        super.onResume()
        searchFragment.listener = this
    }

    override fun onPause() {
        logger.onPause()

        if (searchFragment.listener === this) {
            searchFragment.listener = null
        }

        super.onPause()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        logger.onSaveInstanceState(bundle)
        super.onSaveInstanceState(bundle)
        bundle.putInt(KEY_LIVE_ROADS_BUTTON_VISIBILITY, mainActionsFragment.liveRoadsButton.visibility)
        bundle.putFloat(KEY_SPEED, speed)
        selectedSearchResultMarkerInfo?.let {
            bundle.putDouble(KEY_SEARCH_RESULT_MARKER_LATITUDE, it.latitude)
            bundle.putDouble(KEY_SEARCH_RESULT_MARKER_LONGITUDE, it.longitude)
        }
    }

    override fun onActionSelected(fragment: MainNavigationBarFragment, action: MainNavigationBarFragment.Action) {
        drawerLayout.closeDrawers()
        when (action) {
            MainNavigationBarFragment.Action.LOGOUT -> listener?.onLogoutRequested(this)
            MainNavigationBarFragment.Action.DEVTOOLS -> startDevToolsActivity()
        }
    }

    override fun onActionSelected(fragment: MainActionsFragment, action: MainActionsFragment.Action) {
        when (action) {
            MainActionsFragment.Action.TOGGLE_NAVIGATION_MENU -> toggleNavigationMenuVisible()
            MainActionsFragment.Action.TOGGLE_PIP -> mainMapsFragment.togglePipVisible()
            MainActionsFragment.Action.SEARCH -> toggleSearchVisible()
            MainActionsFragment.Action.GO_TO_MY_LOCATION -> mainMapsFragment.isFollowMeEnabled = true
            MainActionsFragment.Action.GO_TO_MY_LOCATION_AND_RESET_CAMERA -> {
                mainMapsFragment.isFollowMeEnabled = true
                mainMapsFragment.resetLargeMapZoomTilt()
            }
        }
    }

    override fun onFollowMeEnabledChanged(fragment: MainMapsFragment) {
        mainActionsFragment.isGoToMyLocationButtonVisible = !fragment.isFollowMeEnabled
    }

    override fun onPipVisibilityChanged(fragment: MainMapsFragment) {
        updateMyLocationScreenPosition()
    }

    override fun onMapClick(fragment: MainMapsFragment) {
        val selectedSearchResultMarkerId = selectedSearchResultMarkerInfo?.id
        selectedSearchResultMarkerInfo = null

        if (isSearchVisible) {
            setSearchVisible(false, true)
        } else if (selectedSearchResultMarkerId != null) {
            mainMapsFragment.largeMapFragment.removeLocationMarkerFromMap(selectedSearchResultMarkerId)
        }
    }

    fun onBackPressed(): Boolean {
        if (isNavigationMenuVisible) {
            isNavigationMenuVisible = false
            return true
        } else if (isSearchVisible) {
            setSearchVisible(false, true)
            return true
        } else {
            return false
        }
    }

    var isNavigationMenuVisible: Boolean
        get() = drawerLayout.isDrawerVisible(mainNavigationBarFragment.view)
        set(visible) {
            if (visible) {
                drawerLayout.openDrawer(mainNavigationBarFragment.view)
            } else {
                drawerLayout.closeDrawer(mainNavigationBarFragment.view)
            }
        }

    fun toggleNavigationMenuVisible() {
        isNavigationMenuVisible = !isNavigationMenuVisible
    }

    private fun startDevToolsActivity() {
        val intent = Intent().apply {
            setClassName(context, "com.liveroads.devtools.DevToolsActivity")
        }
        intent.putExtra("liveroads.DevToolsActivity.EXTRA_SHOW_BACK_IN_TOOLBAR", true)
        if (devTools.isEnabled) {
            startActivity(intent)
        }
    }

    private fun toggleSearchVisible() {
        val isSearchVisible = !searchFragment.isHidden
        setSearchVisible(!isSearchVisible, true)
    }

    private fun setSearchVisible(visible: Boolean, animate: Boolean) {
        if (visible == !searchFragment.isHidden) {
            return
        }

        fragmentManager.beginTransaction().also {
            if (visible) {
                it.show(searchFragment)
            } else {
                it.hide(searchFragment)
            }
            if (animate) {
                it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
        }.commitNow()

        if (visible) {
            mainActionsFragment.liveRoadsButton.visibility = View.INVISIBLE
            mainMapsFragment.onSearchFragmentDisplayed()
        } else {
            mainActionsFragment.liveRoadsButton.visibility = View.VISIBLE
            mainMapsFragment.onSearchFragmentHidden()
        }

        updateMyLocationScreenPosition()
    }

    companion object {
        private lateinit var firebaseAuth: FirebaseAuth
        private lateinit var firebaseDb: FirebaseDatabase
        private lateinit var navHistory:NavigationHistory
        lateinit var instanceObj:MainFragment

        var ACTION_RECENT_SEARCHING_HISTORY_CHANGE:String = "ACTION_RECENT_SEARCHING_HISTORY_CHANGE"
        var ACTION_HOME_WORK_ADDRESS_CHANGE:String = "ACTION_HOME_WORK_ADDRESS_CHANGED"
        var ACTION_USER_LOCATION_CHANGE:String = "ACTION_USER_LOCATION_CHANGED"

        fun getHistory():ArrayList<NavigationItem>
        {
            return navHistory?.history
        }
    }

    override fun onSearchResultSelected(fragment: SearchFragment, searchResult: SearchResult) {
        logger.logLifecycle("onSearchResultSelected() searchResult=%s", searchResult)

        val latitude = searchResult.latitude
        val longitude = searchResult.longitude
        if (latitude.isNaN() || longitude.isNaN()) {
            return
        }

        navHistory.addRecord(searchResult.label, searchResult.province, searchResult.latitude, searchResult.longitude, searchResult.distance!!)

        setSearchVisible(false, true)
        addSelectedSearchResultMarker(latitude, longitude)
    }

    override fun onSearchPOIResultSelected(fragment: SearchFragment, searchResult: SearchResult) {
        val latitude = searchResult.latitude
        val longitude = searchResult.longitude
        if (latitude.isNaN() || longitude.isNaN()) {
            return
        }


    }

    private fun addSelectedSearchResultMarker(latitude: Double, longitude: Double) {
        selectedSearchResultMarkerInfo?.id?.let {
            mainMapsFragment.largeMapFragment.removeLocationMarkerFromMap(it)
        }
        selectedSearchResultMarkerInfo = null

        mainMapsFragment.largeMapFragment.setMapLocation(true, latitude, longitude, 0.0)

        val markerInfo = SearchResultMarkerInfo(latitude, longitude)
        selectedSearchResultMarkerInfo = markerInfo
        mainMapsFragment.largeMapFragment.addLocationMarkerToMap(markerInfo.id, latitude, longitude)
    }

    private fun updateMapGridLinesVisibility() {
        val visibility = if (mapGridLinesEnabledMonitor.value) View.VISIBLE else View.GONE
        gridLinesView.visibility = visibility
    }

    private fun updateMyLocationScreenPosition() {
        mainMapsFragment.largeMapMyLocationScreenPosition = calculateMyLocationScreenPosition()
    }

    private fun calculateMyLocationScreenPosition(): MyLocationScreenPosition {
        return if (mainMapsFragment.isPipVisible) {
            MyLocationScreenPosition.ABOVE_STREET_NAME
        } else if (isSearchVisible) {
            MyLocationScreenPosition.ABOVE_STREET_NAME
        } else if (speed >= SPEED_MY_LOCATION_MOVE_THRESHOLD) {
            MyLocationScreenPosition.ABOVE_STREET_NAME
        } else {
            MyLocationScreenPosition.CENTER
        }
    }

    private fun updateSpeed() {
        val speed = locationProviderFragment.location?.speed ?: Float.NaN
        if (!speed.isNaN() && speed >= 0) {
            this.speed = speed
        }
    }

    override fun onLocationChanged(fragment: LocationProviderFragment, location: Location) {
        updateSpeed()
        updateMyLocationScreenPosition()
    }

    override fun onStreetInfoChanged(fragment: LocationProviderFragment,
                                     newStreetName: String?,
                                     newMaxSpeed: Double?)
    {
        //pass
    }

    override fun onSpeedChanged(fragment: LocationProviderFragment, speed: Double) {
        //pass
    }

    interface Listener {
        fun onLogoutRequested(fragment: MainFragment)
    }

    private class SearchResultMarkerInfo(val latitude: Double, val longitude: Double) {
        val id = Binder()
    }

}

private const val KEY_LIVE_ROADS_BUTTON_VISIBILITY = "liveroads.MainFragment.KEY_LIVE_ROADS_BUTTON_VISIBILITY"
private const val KEY_SPEED = "liveroads.MainFragment.KEY_SPEED"
private const val KEY_SEARCH_RESULT_MARKER_LATITUDE = "liveroads.MainFragment.KEY_SEARCH_RESULT_MARKER_LATITUDE"
private const val KEY_SEARCH_RESULT_MARKER_LONGITUDE = "liveroads.MainFragment.KEY_SEARCH_RESULT_MARKER_LONGITUDE"
private const val SPEED_MY_LOCATION_MOVE_THRESHOLD = 5.0f / 3.6f
