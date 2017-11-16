package com.liveroads.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.location.Location
import android.os.*
import android.support.annotation.MainThread
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.util.SimpleArrayMap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.liveroads.app.adviser.VoiceAdviser
import com.liveroads.common.devtools.devTools
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.client.monitor.DevToolsSettingMonitor
import com.liveroads.devtools.client.monitor.DevToolsStringSettingMonitor
import com.liveroads.devtools.client.monitor.MapTiltZoomEnabledMonitor
import com.liveroads.location.LocationProviderFragment
import com.liveroads.ui.LiveRoadsMapboxView
import com.liveroads.util.log.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnGestureListener
import com.mapbox.mapboxsdk.maps.OnGestureListener.ZoomSideEffects
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.mapbox.mapboxsdk.style.sources.VectorSource
import com.mapbox.services.android.navigation.v5.MapboxNavigation
import com.mapbox.services.android.navigation.v5.MapboxNavigationOptions
import com.mapbox.services.api.directions.v5.models.DirectionsResponse
import com.mapbox.services.commons.models.Position
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

abstract class LiveRoadsMapFragment(
        val initialCameraPosition: ZoomTilt,
        val styleUrl: String
) : Fragment(), OnMapReadyCallback, LocationProviderFragment.Listener
{

    private lateinit var roadVisibilityOperator: NavigationRoadVisibilityOperator
    private lateinit var navigationMapRoute: NavigationMapRoute

    var mapLocation: MapLocation? = null
        get() = field
        set(value) {
            field = value
            val map = this.map ?: return
            initializeMap(map, value)
            if (value != null) {
                updateLocation(value, animated = true)
            }
        }

    var streetName: String? = null
        get() = field
        set(value) {
            field = value
            updateStreetName()
        }

    var maxSpeed: Double? = null
        get() = field
        set(value) {
            field = value
        }

    var myLocationMarkerXOffsetPercentage = Float.NaN
        set(value) {
            field = value
            updateStreetNameBias()
        }

    var listeners = mutableListOf<Listener>()

    val cameraLocation: Location?
        get() {
            val map = this.map ?: return null
            val latLng = map.cameraPosition?.target ?: return null
            return Location("CameraPosition").apply {
                latitude = latLng.latitude
                longitude = latLng.longitude
                bearing = map.cameraPosition.bearing.toFloat()
            }
        }

    val mapLocationAtMyLocationXY: MapLocation?
        get() {
            val map = this.map ?: return null
            val animator = this.cameraPositionAnimator ?: return null
            val myLocationXY = PointF(animator.myLocationX, animator.myLocationY)
            val latLng = map.projection.fromScreenLocation(myLocationXY) ?: return null
            return MapLocation(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    altitude = latLng.altitude,
                    bearing = map.cameraPosition.bearing
            )
        }

    var forcedZoom = Double.NaN
        set(value) {
            field = value
            mainHandler.sendMessageIfNotQueued(MainHandler.Op.ON_FORCED_CAMERA_SETTINGS_CHANGED)
        }
    var forcedTilt = Double.NaN
        set(value) {
            field = value
            mainHandler.sendMessageIfNotQueued(MainHandler.Op.ON_FORCED_CAMERA_SETTINGS_CHANGED)
        }

    val mapZoom: Double
        get() = map?.cameraPosition?.zoom ?: Double.NaN

    private val logger = obtainLogger()
    protected val liveRoadsMapView: LiveRoadsMapboxView
        get() = (view as LiveRoadsMapboxView)
    private val mapView: MapView
        get() = liveRoadsMapView.mapContainer.getChildAt(0) as MapView
    private val mapState = MapState()
    private val styleUrlMonitorListener = object : DevToolsSettingMonitor.Listener {
        override fun onValueChanged(monitor: DevToolsSettingMonitor<*>) {
            updateMapStyleUrl()
        }
    }

    abstract val styleUrlMonitor: DevToolsStringSettingMonitor
    private lateinit var curStyleUrl: String
    private lateinit var locationMarkers: SimpleArrayMap<IBinder, LocationMarkerInfo>

    private val mapTiltZoomEnabledMonitor = object : MapTiltZoomEnabledMonitor(devTools) {
        override fun onValueChanged() {
            updateDebugCameraInfoVisibility()
            updateDebugCameraInfo()
        }
    }

    private lateinit var selfRef: WeakReference<LiveRoadsMapFragment>
    private lateinit var mainHandler: MainHandler

    private var map: MapboxMap? = null
    private var mapboxMapListener: MyMapboxMapListener? = null
    private var myLocationMarker: Marker? = null
    private var cameraPositionAnimator: CameraPositionAnimator? = null
    private var isStarted = false

    var isTouchEventsEnabled: Boolean
        get() = liveRoadsMapView.isTouchEventsEnabled
        set(value) {
            liveRoadsMapView.isTouchEventsEnabled = value
        }

    var isStreetNameVisible: Boolean
        get() = mapState.isStreetNameVisible
        set(value) {
            mapState.isStreetNameVisible = value
            updateStreetName()
        }

    var isSpeedLimitSignVisible: Boolean
        get() = mapState.isSpeedLimitSignVisible
        set(value) {
            mapState.isSpeedLimitSignVisible = value
        }

    var isPannedAwayFromLocation: Boolean
        get() = mapState.isPannedAway
        set(value) {
            mapState.isPannedAway = value
            updateStreetName()
            listeners.forEach {
                it.onIsPannedAwayFromLocationChanged(this)
            }
        }

    var myLocationScreenPosition: MyLocationScreenPosition
        get() = mapState.myLocationScreenPosition
        set(value) {
            if (value != mapState.myLocationScreenPosition) {
                mapState.myLocationScreenPosition = value
                updateLocation(animated = true)
            }
        }

    abstract val myLocationAboveStreetNameMarginBottomResId: Int
    var isLocationChangeAnimationEnabled = true
    private var mapboxAnimationCallbacks = mutableListOf<MapboxMapAnimationCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        selfRef = WeakReference(this)
        mapState.restore(savedInstanceState)
        mainHandler = MainHandler(selfRef)
    }

    override fun onDestroy() {
        logger.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        selfRef.clear()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.lr_fragment_liveroads_map, container, false) as LiveRoadsMapboxView
        val mapView = onCreateMapView(inflater, view.mapContainer, savedInstanceState)
        mapView.setStyleUrl(styleUrl)
        curStyleUrl = styleUrl
        view.mapContainer.addView(mapView)
        view.txtStreetName.visibility = View.INVISIBLE
        return view
    }

    abstract fun onCreateMapView(inflater: LayoutInflater, container: FrameLayout, savedInstanceState: Bundle?): MapView

    override fun onDestroyView() {
        logger.onDestroyView()
        mapView.onDestroy()
        styleUrlMonitor.removeListener(styleUrlMonitorListener)
        map = null
        myLocationMarker = null
        cameraPositionAnimator?.cancel()
        cameraPositionAnimator = null
        locationMarkers.clear()
        mapboxAnimationCallbacks.clear()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        logger.onViewCreated(view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)
        locationMarkers = SimpleArrayMap()
        styleUrlMonitor.addListener(styleUrlMonitorListener)
        mapView.onCreate(savedInstanceState)
        mapboxAnimationCallbacks = mutableListOf<MapboxMapAnimationCallback>()

        liveRoadsMapView.placeholderAboveStreetNameView.let {
            val bottomMargin = resources.getDimensionPixelSize(myLocationAboveStreetNameMarginBottomResId)
            val lp = it.layoutParams as ViewGroup.MarginLayoutParams
            lp.bottomMargin = bottomMargin
            it.layoutParams = lp
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        logger.onSaveInstanceState(bundle)
        super.onSaveInstanceState(bundle)
        mapView.onSaveInstanceState(bundle)
        mapState.save(bundle)
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()
        isStarted = true
        styleUrlMonitor.start()
        updateMapStyleUrl()
        mapView.onStart()
        mapTiltZoomEnabledMonitor.start()
        updateDebugCameraInfoVisibility()
        updateStreetName()
        updateStreetNameBias()
        updateLocation(animated = false)
        cameraPositionAnimator?.resume()
        applyPendingMapLocation()

        //removed by Yarov
        (parentFragment as MainMapsFragment).locationProviderFragment.addListener(this)
    }

    override fun onStop() {
        logger.onStop()
        cameraPositionAnimator?.pause()
        mapTiltZoomEnabledMonitor.stop()
        map?.setOnMyLocationChangeListener(null)
        mapView.onStop()
        styleUrlMonitor.stop()
        isStarted = false
        (parentFragment as MainMapsFragment).locationProviderFragment.removeListener(this)
        super.onStop()
    }

    override fun onResume() {
        logger.onResume()
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        logger.onPause()
        mapView.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onMapReady(map: MapboxMap) {
        logger.logLifecycle("onMapReady()")
        this.map = map
        this.cameraPositionAnimator = CameraPositionAnimator(map)

        initializeMap(map, mapLocation)
        addLiveRoadsTileSource(map)

        mapboxMapListener = MyMapboxMapListener().also {
            it.register(map)
        }

        applyPendingMapLocation()
        updateDebugCameraInfo()

        for (i in 0..locationMarkers.size() - 1) {
            val markerInfo = locationMarkers.valueAt(i)
            markerInfo.marker = addLocationMarkerToMap(markerInfo.latitude, markerInfo.longitude)
        }

        val navigationOptions = MapboxNavigationOptions().apply {
            setDefaultMilestonesEnabled(true)
        }
        val navigation = MapboxNavigation(
                context,
                Mapbox.getAccessToken(),
                navigationOptions)

        navigationMapRoute = NavigationMapRoute(navigation, mapView, map)
        roadVisibilityOperator =
                NavigationRoadVisibilityOperator(navigationMapRoute, hdZoomTilt())

        /**
         * Thirtieth St at Akron Rd
        Toronto, ON M8W 3C3
        43.600142, -79.531867
         */
        val origin = Position.fromLngLat(-79.531867, 43.600142)

        /**
         * 1501-1503 Dixie Rd
        Mississauga, ON L5E
        43.593742, -79.564199
         */
        val destination = Position.fromLngLat(-79.564199, 43.593742)
        requestDirectionsRoute(origin, destination)
    }

    private fun requestDirectionsRoute(origin: Position, destination: Position) {
        val directions = MapboxUtils.createDirections(origin, destination)

        directions.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>?, response: Response<DirectionsResponse>?) {
                // fixme: cleanup
                if (response!!.body() != null) {
                    if (response.body()!!.routes.size > 0) {
                        val route = response.body()!!.routes[0]
                        navigationMapRoute.addRoute(route)
                        VoiceAdviser.start()
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>?, t: Throwable?) {
                // fixme: cleanup. make failures consistent
                Log.e("","fail", t)
            }
        })
    }

    private fun drawableToIcon(context: Context, id: Int, colorRes: Int): Icon {
        val drawable = ContextCompat.getDrawable(context, id)
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        DrawableCompat.setTint(drawable, colorRes)
        drawable.draw(canvas)
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    private fun updateLocation(animated: Boolean) {
        if (map != null && isStarted) {
            mapLocation?.let { location ->
                updateLocation(location, animated)
            }
        }
    }

    private fun updateLocation(location: MapLocation, animated: Boolean) {
        val animator = cameraPositionAnimator!!

        val myLocationMarker = myLocationMarker!!
        myLocationMarker.position = location.toLatLng()

        val mapboxAnimationCallback = getRelevantMapboxAnimationCallback()
        if (mapboxAnimationCallback != null && mapboxAnimationCallback.isFinished) {
            // this is a HACK to work around the situation where the "my location" blue dot is not at the exactly-correct
            // position after a mapbox animation; this is due to the imperfect calculation of lat/lon center position
            // to cause the "my location" blue dot to be away from the center point
            animator.updateLastValuesFromMap(mapboxAnimationCallback.latLng)
        }
        val isMapboxAnimationInProgress = (mapboxAnimationCallback != null && !mapboxAnimationCallback.isFinished)

        if (isFollowMeEnabled && !isMapboxAnimationInProgress) {

            val myLocationPlaceholder = when (myLocationScreenPosition) {
                MyLocationScreenPosition.CENTER -> LiveRoadsMapboxView.Placeholder.CENTER
                MyLocationScreenPosition.ABOVE_STREET_NAME -> LiveRoadsMapboxView.Placeholder.ABOVE_STREET_NAME
            }
            val myLocationXY = liveRoadsMapView.getPlaceholderLocation(myLocationPlaceholder)

            val isMyLocationXYChanged = animator.myLocationX != myLocationXY.x || animator.myLocationY != myLocationXY.y

            animator.myLocationX = myLocationXY.x
            animator.myLocationY = myLocationXY.y
            animator.latitude = location.latitude
            animator.longitude = location.longitude
            animator.altitude = location.altitude
            animator.bearing = location.bearing
            animator.tilt = map!!.cameraPosition.tilt
            animator.zoom = map!!.cameraPosition.zoom

            if (!isLocationChangeAnimationEnabled || !animated || !animator.isInitialized) {
                animator.cancel()
                animator.updateMap()
                animator.captureStartValues()
            } else if (isMyLocationXYChanged) {
                animator.cancel()
                animator.captureStartValues()
                animator.start()
            } else if (animator.isRunning) {
                animator.captureStartValues()
            } else {
                animator.start()
            }
        }

        updateDebugCameraInfoAsync()

        LocalBroadcastManager.getInstance(this.context).sendBroadcast(Intent("ACTION_USER_LOCATION_CHANGED"))
    }

    private fun getRelevantMapboxAnimationCallback(): MapboxMapAnimationCallback? {
        var lastCallback: MapboxMapAnimationCallback? = null
        for (i in mapboxAnimationCallbacks.size - 1 downTo 0) {
            val callback = mapboxAnimationCallbacks[i]
            if (callback.isFinished) {
                if (lastCallback == null || callback.finishTime > lastCallback.finishTime) {
                    lastCallback = callback
                }
                mapboxAnimationCallbacks.removeAt(i)
            } else {
                return callback
            }
        }
        return lastCallback
    }

    private fun onForcedCameraSettingsChanged() {
        val map = map ?: return

        var changed = false

        val newCameraPosition = CameraPosition.Builder().run {
            if (!forcedZoom.isNaN()) {
                zoom(forcedZoom)
                cameraPositionAnimator?.zoom = forcedZoom
                changed = true
            }
            if (!forcedTilt.isNaN()) {
                tilt(forcedTilt)
                cameraPositionAnimator?.tilt = forcedTilt
                changed = true
            }
            build()
        }

        if (changed) {
            map.cameraPosition = newCameraPosition
            cameraPositionAnimator?.captureStartValues()
        }
    }

    private fun updateDebugCameraInfoVisibility() {
        val visibility = if (mapTiltZoomEnabledMonitor.value) View.VISIBLE else View.GONE
        liveRoadsMapView.debugInfoViewGroup.visibility = visibility
        updateDebugCameraInfo()
    }

    private fun updateMapStyleUrl() {
        val newStyleUrl = styleUrlMonitor.value ?: styleUrl
        if (newStyleUrl == curStyleUrl) {
            return
        }

        logger.d("updateMapStyleUrl() setting URL: %s", newStyleUrl)

        val map = this.map
        if (map == null) {
            mapView.setStyleUrl(newStyleUrl)
        } else {
            map.setStyleUrl(newStyleUrl) {
                addLiveRoadsTileSource(map)
            }
        }

        curStyleUrl = newStyleUrl
    }

    private fun addLiveRoadsTileSource(map: MapboxMap) {
        map.addSource(VectorSource("lr-tile-source",
                TileSet("2.1.0", "http://45.55.242.94/walid/lr/tile2.php?/{z}/{x}/{y}.mvt").apply {
                    maxZoom = 18f
                    minZoom = 16f
                }))
    }

    private fun updateDebugCameraInfoAsync() {
        if (mapTiltZoomEnabledMonitor.value) {
            mainHandler.sendMessageDelayedIfNotQueued(MainHandler.Op.UPDATE_CAMERA_DEBUG_INFO, 30)
        }
    }

    private fun updateDebugCameraInfo() {
        if (!mapTiltZoomEnabledMonitor.value) {
            return
        }
        val cp = map?.cameraPosition ?: return
        liveRoadsMapView.txtDebugLatitude.text = getString(R.string.lr_debug_map_latitude_amount, cp.target.latitude)
        liveRoadsMapView.txtDebugLongitude.text = getString(R.string.lr_debug_map_longitude_amount, cp.target.longitude)
        liveRoadsMapView.txtDebugAltitude.text = getString(R.string.lr_debug_map_altitude_amount, cp.target.altitude)
        liveRoadsMapView.txtDebugBearing.text = getString(R.string.lr_debug_map_bearing_amount, cp.bearing)
        liveRoadsMapView.txtDebugTilt.text = getString(R.string.lr_debug_map_tilt_amount, cp.tilt)
        liveRoadsMapView.txtDebugZoom.text = getString(R.string.lr_debug_map_zoom_amount, cp.zoom)
    }

    var isFollowMeEnabled: Boolean
        get() = mapState.isFollowMeEnabled
        set(enabled) {
            mapState.isFollowMeEnabled = enabled

            if (enabled) {
                isPannedAwayFromLocation = false
                val map = this.map
                val location = mapLocation
                if (map != null && location != null) {
                    val animator = cameraPositionAnimator!!
                    animator.latitude = location.latitude
                    animator.longitude = location.longitude
                    animator.altitude = location.altitude
                    animator.bearing = location.bearing
                    val calculatedPosition = animator.calculatePosition()
                    val latLng = LatLng(calculatedPosition.latitude, calculatedPosition.longitude)
                    val cameraUpdate = CameraUpdateFactory.newLatLng(latLng)
                    val animationCallback = MapboxMapAnimationCallback(latLng)
                    mapboxAnimationCallbacks.add(animationCallback)
                    map.animateCamera(cameraUpdate, 1000, animationCallback)
                }
            } else {
                cameraPositionAnimator?.cancel()
            }

            listeners.forEach {
                it.onFollowMeEnabledChanged(this)
            }
        }

    private fun initializeMap(map: MapboxMap, location: MapLocation?) {
        if (location != null && myLocationMarker == null) {
            val icon = drawableToIcon(context, R.drawable.mapbox_mylocation_icon_default,
                    ResourcesCompat.getColor(resources, R.color.mapbox_blue, context.theme))
            val markerOptions = MarkerOptions()
                    .position(LatLng(location.toLatLng()))
                    .icon(icon)
            myLocationMarker = map.addMarker(markerOptions)
        }

        if (!mapState.isOriented) {
            map.cameraPosition = CameraPosition.Builder().run {
                zoom(if (forcedZoom.isNaN()) initialCameraPosition.zoom else forcedZoom)
                tilt(if (forcedTilt.isNaN()) initialCameraPosition.tilt else forcedTilt)
                build()
            }
            mapState.isOriented = true
        }

        if (!mapState.isPositioned && location != null) {
            updateLocation(location, animated = false)
            mapState.isPositioned = true
        }
    }

    private fun updateStreetName() {
        if (!isStarted) {
            return
        }
        val streetNameVisible = isStreetNameVisible
                && !isPannedAwayFromLocation
                && !streetName.isNullOrBlank()
        liveRoadsMapView.let {
            it.txtStreetName.text = if (streetNameVisible) streetName else null
            it.txtStreetName.visibility = if (streetNameVisible) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updateStreetNameBias() {
        if (!isStarted) {
            return
        }
        liveRoadsMapView.streetNameXBias = myLocationMarkerXOffsetPercentage
    }

    fun addLocationMarkerToMap(id: IBinder, latitude: Double, longitude: Double) {
        val marker = addLocationMarkerToMap(latitude, longitude)
        val markerInfo = LocationMarkerInfo(latitude, longitude, marker)
        locationMarkers.put(id, markerInfo)
    }

    private fun addLocationMarkerToMap(latitude: Double, longitude: Double): Marker? {
        val markerOptions = MarkerOptions().also {
            val position = LatLng(latitude, longitude)
            val iconFactory = IconFactory.getInstance(context)
            val icon = iconFactory.fromResource(R.drawable.mapbox_marker_icon_default)

            it.position = position
            it.icon = icon
        }

        return map?.addMarker(markerOptions)
    }

    private data class LocationMarkerInfo(val latitude: Double, val longitude: Double, var marker: Marker?)

    fun removeLocationMarkerFromMap(id: IBinder) {
        val index = locationMarkers.indexOfKey(id)
        if (index < 0) {
            return
        }

        val markerInfo = locationMarkers.valueAt(index)
        locationMarkers.removeAt(index)

        val marker = markerInfo.marker ?: return
        map?.removeMarker(marker)
    }

    private fun applyPendingMapLocation() {
        val latitude = mapState.pendingMapLatitude
        val longitude = mapState.pendingMapLongitude
        val bearing = mapState.pendingMapBearing
        mapState.pendingMapLatitude = Double.NaN
        mapState.pendingMapLongitude = Double.NaN
        mapState.pendingMapBearing = Double.NaN

        if (!(latitude.isNaN() || longitude.isNaN() || bearing.isNaN())) {
            setMapLocation(false, latitude, longitude, bearing)
        }
    }

    fun setMapLocation(animated: Boolean, latitude: Double, longitude: Double, bearing: Double) {
        val map = map

        if (!isStarted || map == null) {
            mapState.pendingMapLatitude = latitude
            mapState.pendingMapLongitude = longitude
            mapState.pendingMapBearing = bearing
            return
        }

        isFollowMeEnabled = false

        if (!animated) {
            map.cameraPosition = CameraPosition.Builder().let {
                it.target(LatLng(latitude, longitude, 0.0))
                it.bearing(bearing)
                it.build()
            }
        } else {
            val latLng = LatLng(latitude, longitude)
            val cameraUpdate = CameraUpdateFactory.newLatLng(latLng)
            val animationCallback = MapboxMapAnimationCallback(latLng)
            mapboxAnimationCallbacks.add(animationCallback)
            map.animateCamera(cameraUpdate, 1000, animationCallback)
        }
    }

    private class MapState {

        var isOriented = false
        var isPositioned = false
        var isFollowMeEnabled = true
        var isStreetNameVisible = true
        var isSpeedLimitSignVisible = false
        var isPannedAway = false
        var myLocationScreenPosition = MyLocationScreenPosition.ABOVE_STREET_NAME
        var pendingMapLatitude = Double.NaN
        var pendingMapLongitude = Double.NaN
        var pendingMapBearing = Double.NaN

        private companion object {
            const val KEY_IS_ORIENTED = "liveroads.LiveRoadsMapFragment.MapState.KEY_IS_ORIENTED"
            const val KEY_IS_POSITIONED = "liveroads.LiveRoadsMapFragment.MapState.KEY_IS_POSITIONED"
            const val KEY_IS_FOLLOW_ME_ENABLED = "liveroads.LiveRoadsMapFragment.MapState.KEY_IS_FOLLOW_ME_ENABLED"
            const val KEY_IS_STREET_NAME_VISIBLE = "liveroads.LiveRoadsMapFragment.MapState.KEY_IS_STREET_NAME_VISIBLE"
            const val KEY_IS_SPEED_LIMIT_SIGN_VISIBLE = "liveroads.LiveRoadsMapFragment.MapState.KEY_IS_SPEED_LIMIT_SIGN_VISIBLE"
            const val KEY_IS_PANNED_AWAY = "liveroads.LiveRoadsMapFragment.MapState.KEY_IS_PANNED_AWAY"
            const val KEY_MY_LOCATION_SCREEN_POSITION = "liveroads.LiveRoadsMapFragment.MapState.KEY_MY_LOCATION_SCREEN_POSITION"
            const val KEY_PENDING_MAP_LATITUDE = "liveroads.LiveRoadsMapFragment.MapState.KEY_PENDING_MAP_LATITUDE"
            const val KEY_PENDING_MAP_LONGITUDE = "liveroads.LiveRoadsMapFragment.MapState.KEY_PENDING_MAP_LONGITUDE"
            const val KEY_PENDING_MAP_BEARING = "liveroads.LiveRoadsMapFragment.MapState.KEY_PENDING_MAP_BEARING"
        }

        fun save(bundle: Bundle) {
            bundle.putBoolean(KEY_IS_ORIENTED, isOriented)
            bundle.putBoolean(KEY_IS_POSITIONED, isPositioned)
            bundle.putBoolean(KEY_IS_FOLLOW_ME_ENABLED, isFollowMeEnabled)
            bundle.putBoolean(KEY_IS_STREET_NAME_VISIBLE, isStreetNameVisible)
            bundle.putBoolean(KEY_IS_SPEED_LIMIT_SIGN_VISIBLE, isSpeedLimitSignVisible)
            bundle.putBoolean(KEY_IS_PANNED_AWAY, isPannedAway)
            bundle.putString(KEY_MY_LOCATION_SCREEN_POSITION, myLocationScreenPosition.name)
            bundle.putDouble(KEY_PENDING_MAP_LATITUDE, pendingMapLatitude)
            bundle.putDouble(KEY_PENDING_MAP_LONGITUDE, pendingMapLongitude)
            bundle.putDouble(KEY_PENDING_MAP_BEARING, pendingMapBearing)
        }

        fun restore(bundle: Bundle?) {
            bundle?.apply {
                isOriented = bundle.getBoolean(KEY_IS_ORIENTED, isOriented)
                isPositioned = bundle.getBoolean(KEY_IS_POSITIONED, isPositioned)
                isFollowMeEnabled = bundle.getBoolean(KEY_IS_FOLLOW_ME_ENABLED, isFollowMeEnabled)
                isStreetNameVisible = bundle.getBoolean(KEY_IS_STREET_NAME_VISIBLE, isStreetNameVisible)
                isSpeedLimitSignVisible = bundle.getBoolean(KEY_IS_SPEED_LIMIT_SIGN_VISIBLE, isSpeedLimitSignVisible)
                isPannedAway = bundle.getBoolean(KEY_IS_PANNED_AWAY, isPannedAway)
                myLocationScreenPosition = bundle.getString(KEY_MY_LOCATION_SCREEN_POSITION,
                        myLocationScreenPosition.name).let {
                    MyLocationScreenPosition.valueOf(it)
                }
                pendingMapLatitude = bundle.getDouble(KEY_PENDING_MAP_LATITUDE, pendingMapLatitude)
                pendingMapLongitude = bundle.getDouble(KEY_PENDING_MAP_LATITUDE, pendingMapLongitude)
                pendingMapBearing = bundle.getDouble(KEY_PENDING_MAP_LATITUDE, pendingMapBearing)
            }
        }

    }

    @MainThread
    private class MainHandler(val fragment: WeakReference<LiveRoadsMapFragment>) : Handler() {

        enum class Op {
            UPDATE_CAMERA_DEBUG_INFO,
            ON_FORCED_CAMERA_SETTINGS_CHANGED,
        }

        override fun handleMessage(message: Message) {
            val op = Op.values()[message.what]
            when (op) {
                Op.UPDATE_CAMERA_DEBUG_INFO -> {
                    fragment.get()?.updateDebugCameraInfo()
                }
                Op.ON_FORCED_CAMERA_SETTINGS_CHANGED -> {
                    fragment.get()?.onForcedCameraSettingsChanged()
                }
            }
        }

        fun sendMessageDelayedIfNotQueued(op: Op, delay: Long) {
            if (!hasMessages(op.ordinal)) {
                sendEmptyMessageDelayed(op.ordinal, delay)
            }
        }

        fun sendMessageIfNotQueued(op: Op) {
            if (!hasMessages(op.ordinal)) {
                sendEmptyMessage(op.ordinal)
            }
        }

    }

    private inner class MyMapboxMapListener :
            MapboxMap.OnCameraMoveStartedListener,
            MapboxMap.OnCameraMoveListener,
            MapboxMap.OnScrollListener,
            MapboxMap.OnMapClickListener,
            OnGestureListener {

        override fun onCameraMoveStarted(reason: Int) {
            if (reason == MapboxMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                isFollowMeEnabled = false
            }
            roadVisibilityOperator.onZoomChanged(map!!.cameraPosition.zoom)
        }

        override fun onCameraMove() {
            listeners.forEach {
                it.onCameraMoved(this@LiveRoadsMapFragment)
            }
            updateDebugCameraInfoAsync()
        }

        override fun onScroll() {
            isFollowMeEnabled = false
            isPannedAwayFromLocation = true
            updateStreetName()
            updateDebugCameraInfoAsync()
        }

        override fun onZoomGestureProgress(zoom: Double, sideEffects: ZoomSideEffects) {
            listeners.forEach {
                it.onZoomGestureProgress(this@LiveRoadsMapFragment, zoom, sideEffects)
            }
        }

        override fun onZoomGestureStarted(zoom: Double) {
            listeners.forEach {
                it.onZoomGestureStarted(this@LiveRoadsMapFragment, zoom)
            }
        }

        override fun onZoomGestureEnded(zoom: Double) {
            listeners.forEach {
                it.onZoomGestureEnded(this@LiveRoadsMapFragment, zoom)
            }
        }

        override fun onTiltGestureStarted(tilt: Double) {
            listeners.forEach {
                it.onTiltGestureStarted(this@LiveRoadsMapFragment, tilt)
            }
        }

        override fun onTiltGestureEnded(tilt: Double) {
            listeners.forEach {
                it.onTiltGestureEnded(this@LiveRoadsMapFragment, tilt)
            }
        }

        override fun onMapClick(point: LatLng) {
            listeners.forEach {
                it.onMapClick(this@LiveRoadsMapFragment)
            }
        }

        fun register(map: MapboxMap) {
            map.setOnCameraMoveListener(this)
            map.setOnCameraMoveStartedListener(this)
            map.setOnScrollListener(this)
            map.setOnGestureListener(this)
            map.setOnMapClickListener(this)
        }

    }

    interface Listener {

        @MainThread
        fun onMapClick(fragment: LiveRoadsMapFragment)

        @MainThread
        fun onCameraMoved(fragment: LiveRoadsMapFragment)

        @MainThread
        fun onFollowMeEnabledChanged(fragment: LiveRoadsMapFragment)

        @MainThread
        fun onIsPannedAwayFromLocationChanged(fragment: LiveRoadsMapFragment)

        @MainThread
        fun onZoomGestureStarted(fragment: LiveRoadsMapFragment, zoom: Double)

        @MainThread
        fun onZoomGestureEnded(fragment: LiveRoadsMapFragment, zoom: Double)

        @MainThread
        fun onZoomGestureProgress(fragment: LiveRoadsMapFragment, zoom: Double, sideEffects: ZoomSideEffects)

        @MainThread
        fun onTiltGestureStarted(fragment: LiveRoadsMapFragment, tilt: Double)

        @MainThread
        fun onTiltGestureEnded(fragment: LiveRoadsMapFragment, tilt: Double)

    }

    enum class MyLocationScreenPosition {
        CENTER,
        ABOVE_STREET_NAME,
    }

    companion object {
        const val HD_ZOOM_TRANSITION_START = 17.5
        const val HD_ZOOM_TRANSITION_END = 18.0
    }

    private class MapboxMapAnimationCallback(val latLng: LatLng) : MapboxMap.CancelableCallback {

        private var isOnFinishInvoked = false
        private var isOnCancelInvoked = false
        val isFinished: Boolean
            get() = isOnFinishInvoked || isOnCancelInvoked
        var finishTime = 0L
            private set

        override fun onFinish() {
            if (!isFinished) {
                finishTime = SystemClock.uptimeMillis()
            }
            isOnFinishInvoked = true
        }

        override fun onCancel() {
            if (!isFinished) {
                finishTime = SystemClock.uptimeMillis()
            }
            isOnCancelInvoked = true
        }

    }

    data class MapLocation(
            var latitude: Double = 0.0,
            var longitude: Double = 0.0,
            var altitude: Double = 0.0,
            var bearing: Double = 0.0
    ) {

        fun toLatLng() = LatLng(latitude, longitude, altitude)

    }

    abstract fun mapZoomTilt(): ZoomTilt

    abstract fun hdZoomTilt(): ZoomTilt

    override fun onLocationChanged(fragment: LocationProviderFragment, location: Location) {
        //pass
    }

    override fun onStreetInfoChanged(fragment: LocationProviderFragment, newStreetName: String?, newMaxSpeed: Double?) {
        //pass
    }

    override fun onSpeedChanged(fragment: LocationProviderFragment, speed: Double) {
        if (speed <= 0) { return }
        val speedKmH = speed * 3.6
        updateMaxSpeedLimit(speedKmH)
    }

    private fun updateMaxSpeedLimit(currentSpeedKmH: Double) {
        if (!isStarted) {
            return
        }

        val ms = maxSpeed
        val canShowSpeedLimit = isSpeedLimitSignVisible && !isPannedAwayFromLocation
        val shouldShowSpeedLimit = ms != null && currentSpeedKmH >= ms
        val speedLimitSignVisible = canShowSpeedLimit && shouldShowSpeedLimit

        liveRoadsMapView.let {
            it.speedLimitSign.visibility = if (speedLimitSignVisible) View.VISIBLE else View.GONE
            if (speedLimitSignVisible) {
                val t = speedText(maxSpeed!!)
                it.speedLimitSign.update(t.first, t.second)
            }
        }
    }

    private fun speedText(speedMS: Double): Pair<String, String> {
        return if (speedMS >= 1000) {
            Pair((speedMS/1000).toInt().toString(), "KM/H")
        } else {
            Pair(speedMS.toInt().toString(), "m/s")
        }
    }

}
