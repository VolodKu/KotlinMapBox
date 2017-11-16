package com.liveroads.app

import android.animation.Animator
import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liveroads.app.LiveRoadsMapFragment.Companion.HD_ZOOM_TRANSITION_START
import com.liveroads.app.LiveRoadsMapFragment.MapLocation
import com.liveroads.app.LiveRoadsMapFragment.MyLocationScreenPosition
import com.liveroads.app.adviser.RoadFollowerListener
import com.liveroads.app.adviser.UserRoadFollower
import com.liveroads.common.devtools.devTools
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.client.monitor.LargeMapCenterAsLocationEnabledMonitor
import com.liveroads.location.LocationProviderFragment
import com.liveroads.ui.EstimateNextTurnView
import com.liveroads.ui.PipView
import com.liveroads.util.log.*
import com.mapbox.mapboxsdk.maps.OnGestureListener.ZoomSideEffects

class MainMapsFragment
    : Fragment(), LiveRoadsMapFragment.Listener, LocationProviderFragment.Listener,
        RoadFollowerListener

{
    override fun onNextTurnInfoChanged(nt: UserRoadFollower.NextTurnInfo) {
        activity?.runOnUiThread {
            if (isAdded) {
                val direction: Long = when(nt.direction) {
                    UserRoadFollower.TURN_DIRECTION.LEFT -> EstimateNextTurnView.LEFT
                    UserRoadFollower.TURN_DIRECTION.RIGHT -> EstimateNextTurnView.RIGHT
                    else -> -1
                }
                pipView.pipEstimateView.updateNextTurn(nt.dist, nt.streetName, direction)
                pipView.largeEstimateView.updateNextTurn(nt.dist, nt.streetName, direction)
            }
        }
    }

    override fun onRouteInfoChanged(ri: UserRoadFollower.RouteInfo) {
        activity?.runOnUiThread({
            if (isAdded) {
                pipView.pipEstimateView.updateTotal(ri.dist, ri.time)
                pipView.largeEstimateView.updateTotal(ri.dist, ri.time)
            }
        })
    }

    var listener: Listener? = null

    var isFollowMeEnabled: Boolean
        get() = largeMapFragment.isFollowMeEnabled
        set(value) {
            largeMapFragment.isFollowMeEnabled = value
        }

    lateinit var locationProviderFragment: LocationProviderFragment

    private val logger = obtainLogger()
    private val pipView: PipView
        get() = view as PipView
    val largeMapFragment: LargeMapFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_main_maps_large_map) as LargeMapFragment
    val pipMapFragment: PipMapFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_main_maps_pip_map) as PipMapFragment

    private val largeMapCenterAsLocationEnabledMonitor = object : LargeMapCenterAsLocationEnabledMonitor(devTools) {
        override fun onValueChanged() {
            updateLargeMapCenterAsLocationEnabled()
        }
    }

    private val isLargeMapCenterBeingUsedAsLocation: Boolean
        get() = locationProviderFragment.forcedLocationProvider === forcedLocationProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_main_maps, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        logger.onViewCreated(view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        pipView.pipVisible = false
        savedInstanceState?.let {
            pipView.pipVisible = it.getBoolean(KEY_PIP_VISIBLE, pipView.pipVisible)
        }

        pipView.pipVisibilityAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animator: Animator) {
                pipMapFragment.isLocationChangeAnimationEnabled = !animator.isRunning
            }

            override fun onAnimationEnd(animator: Animator) {
                pipMapFragment.isLocationChangeAnimationEnabled = !animator.isRunning
            }

            override fun onAnimationCancel(animator: Animator) {
                pipMapFragment.isLocationChangeAnimationEnabled = !animator.isRunning
            }

            override fun onAnimationStart(animator: Animator) {
                pipMapFragment.isLocationChangeAnimationEnabled = !animator.isRunning
            }
        })

        updateMyMarkerOffsets()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
        largeMapFragment.isTouchEventsEnabled = true
        pipMapFragment.isTouchEventsEnabled = false
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        logger.onSaveInstanceState(bundle)
        super.onSaveInstanceState(bundle)
        bundle.putBoolean(KEY_PIP_VISIBLE, pipView.pipVisible)
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()
        locationProviderFragment.addListener(this)
        largeMapFragment.listeners.add(this)
        largeMapCenterAsLocationEnabledMonitor.start()
        updateLargeMapCenterAsLocationEnabled()
        updatePipCameraSettings()
        updateLocation()
        updateStreetName()
        updateMaxSpeed()
        UserRoadFollower.addListener(this)
    }

    override fun onStop() {
        logger.onStop()
        if (isLargeMapCenterBeingUsedAsLocation) {
            locationProviderFragment.forcedLocationProvider = null
        }
        largeMapCenterAsLocationEnabledMonitor.stop()
        largeMapFragment.listeners.remove(this)
        locationProviderFragment.removeListener(this)
        UserRoadFollower.removeListener(this)
        super.onStop()
    }

    fun togglePipVisible() {
        pipView.animateTogglePipVisible()
        updateMyMarkerOffsets()
        listener?.onPipVisibilityChanged(this)
    }

    var isPipVisible: Boolean
        get() = pipView.pipVisible
        set(visible) {
            pipView.animateSetPipVisible(visible)
            updateMyMarkerOffsets()
            listener?.onPipVisibilityChanged(this)
        }

    private fun updateMyMarkerOffsets() {
        largeMapFragment.myLocationMarkerXOffsetPercentage = if (pipView.pipVisible) {
            0.25f
        } else {
            DEFAULT_MY_LOCATION_MARKER_X_OFFSET_PERCENTAGE
        }
        pipMapFragment.myLocationMarkerXOffsetPercentage = DEFAULT_MY_LOCATION_MARKER_X_OFFSET_PERCENTAGE
    }

    private fun updateLargeMapCenterAsLocationEnabled() {
        val enabled = largeMapCenterAsLocationEnabledMonitor.value && !largeMapFragment.isFollowMeEnabled
        if (enabled) {
            locationProviderFragment.forcedLocationProvider = forcedLocationProvider
        } else if (locationProviderFragment.forcedLocationProvider === forcedLocationProvider) {
            locationProviderFragment.forcedLocationProvider = null
        }
    }

    private val isPipFollowLargeMapEnabled: Boolean
        get() = (!largeMapFragment.isFollowMeEnabled && largeMapFragment.mapZoom >= HD_ZOOM_TRANSITION_START)

    private fun updatePipCameraSettings() {
        val largeMapZoom = largeMapFragment.mapZoom
        if (largeMapZoom.isNaN()) {
            return
        }

        val zoomTilt = if (largeMapZoom < HD_ZOOM_TRANSITION_START) {
            PipMapFragment.HD_ZOOM_TILT
        } else {
            PipMapFragment.MAP_ZOOM_TILT
        }

        pipMapFragment.animateSetZoomTilt(zoomTilt)
    }

    override fun onCameraMoved(fragment: LiveRoadsMapFragment) {
        if (fragment !== largeMapFragment) {
            throw IllegalArgumentException("unexpected fragment: $fragment")
        }
        if (isLargeMapCenterBeingUsedAsLocation) {
            locationProviderFragment.onLocationChanged(false)
        }
        updatePipCameraSettings()

        if (isPipFollowLargeMapEnabled) {
            updatePipMapLocation()
        }
    }

    override fun onIsPannedAwayFromLocationChanged(fragment: LiveRoadsMapFragment) {
        if (fragment !== largeMapFragment) {
            throw IllegalArgumentException("unexpected fragment: $fragment")
        }
        pipMapFragment.isPannedAwayFromLocation = fragment.isPannedAwayFromLocation
    }

    override fun onFollowMeEnabledChanged(fragment: LiveRoadsMapFragment) {
        if (fragment !== largeMapFragment) {
            throw IllegalArgumentException("unexpected fragment: $fragment")
        }
        updateLargeMapCenterAsLocationEnabled()
        updateLocation()
        listener?.onFollowMeEnabledChanged(this)
    }

    override fun onZoomGestureStarted(fragment: LiveRoadsMapFragment, zoom: Double) {
    }

    override fun onZoomGestureEnded(fragment: LiveRoadsMapFragment, zoom: Double) {
    }

    override fun onZoomGestureProgress(fragment: LiveRoadsMapFragment, zoom: Double, sideEffects: ZoomSideEffects) {
    }

    override fun onTiltGestureStarted(fragment: LiveRoadsMapFragment, tilt: Double) {
    }

    override fun onTiltGestureEnded(fragment: LiveRoadsMapFragment, tilt: Double) {
    }

    override fun onMapClick(fragment: LiveRoadsMapFragment) {
        listener?.onMapClick(this)
    }

    fun resetLargeMapZoomTilt() {
        largeMapFragment.resetZoomTilt()
    }

    fun onSearchFragmentDisplayed() {
        isPipVisible = false
        largeMapFragment.forcedZoom = LargeMapFragment.SEARCH_DISPLAYED_ZOOM_TILT.zoom
        largeMapFragment.forcedTilt = LargeMapFragment.SEARCH_DISPLAYED_ZOOM_TILT.tilt
    }

    fun onSearchFragmentHidden() {
        largeMapFragment.forcedZoom = LargeMapFragment.SEARCH_HIDDEN_ZOOM_TILT.zoom
        largeMapFragment.forcedTilt = LargeMapFragment.SEARCH_HIDDEN_ZOOM_TILT.tilt
    }

    private companion object {
        val DEFAULT_MY_LOCATION_MARKER_X_OFFSET_PERCENTAGE = Float.NaN
        const val KEY_PIP_VISIBLE = "liveroads.PipMapFragment.KEY_PIP_VISIBLE"
    }

    private val forcedLocationProvider = object : LocationProviderFragment.ForcedLocationProvider {

        override val location: Location?
            get() = largeMapFragment.cameraLocation

    }

    var largeMapMyLocationScreenPosition: MyLocationScreenPosition
        get() = largeMapFragment.myLocationScreenPosition
        set(value) {
            largeMapFragment.myLocationScreenPosition = value
        }

    override fun onLocationChanged(fragment: LocationProviderFragment, location: Location) {
        updateLocation()
        UserRoadFollower.updateCurrentLocation(location.longitude, location.latitude)
    }

    override fun onStreetInfoChanged(fragment: LocationProviderFragment,
                                     newStreetName: String?,
                                     newMaxSpeed: Double?)
    {
        updateStreetName()
        updateMaxSpeed()
    }

    override fun onSpeedChanged(fragment: LocationProviderFragment, speed: Double) {
        UserRoadFollower.updateSpeed(speed)
    }

    private fun updateLocation() {
        updateLargeMapLocation()
        updatePipMapLocation()
    }

    private fun updateLargeMapLocation() {
        val location = locationProviderFragment.location
        largeMapFragment.mapLocation = if (location == null) {
            null
        } else {
            (largeMapFragment.mapLocation ?: MapLocation()).setValuesFrom(location)
        }
    }

    private fun updatePipMapLocation() {
        pipMapFragment.mapLocation = if (isPipFollowLargeMapEnabled) {
            largeMapFragment.mapLocationAtMyLocationXY
        } else {
            val location = locationProviderFragment.location
            if (location == null) {
                null
            } else {
                (pipMapFragment.mapLocation ?: MapLocation()).setValuesFrom(location)
            }
        }
    }

    private fun updateStreetName() {
        largeMapFragment.streetName = locationProviderFragment.streetName
        pipMapFragment.streetName = locationProviderFragment.streetName
    }

    private fun updateMaxSpeed() {
        largeMapFragment.maxSpeed = locationProviderFragment.maxSpeed
    }

    interface Listener {
        fun onFollowMeEnabledChanged(fragment: MainMapsFragment)
        fun onMapClick(fragment: MainMapsFragment)
        fun onPipVisibilityChanged(fragment: MainMapsFragment)
    }

}

private fun MapLocation.setValuesFrom(location: Location): MapLocation {
    latitude = location.latitude
    longitude = location.longitude
    altitude = location.altitude
    bearing = location.bearing.toDouble()
    return this
}
