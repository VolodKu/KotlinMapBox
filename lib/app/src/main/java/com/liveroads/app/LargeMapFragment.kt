package com.liveroads.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.liveroads.common.devtools.devTools
import com.liveroads.devtools.client.monitor.LargeMapStyleUrlMonitor
import com.liveroads.util.squared
import com.mapbox.mapboxsdk.constants.MapboxConstants
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnGestureListener.ZoomSideEffects

class LargeMapFragment : LiveRoadsMapFragment(
        initialCameraPosition = MAP_ZOOM_TILT,
        styleUrl = "http://45.55.242.94/walid/lr/css/dayhd.json"
) {

    init {
        isStreetNameVisible = true
        isSpeedLimitSignVisible = true
    }

    override val styleUrlMonitor = LargeMapStyleUrlMonitor(devTools)
    override val myLocationAboveStreetNameMarginBottomResId = R.dimen.lr_my_location_margin_bottom_large_map
    private var tiltOnZoomListener: TiltOnZoomListener? = null
    private var isStarted = false

    override fun onCreateMapView(inflater: LayoutInflater, container: FrameLayout, savedInstanceState: Bundle?): MapView {
        return inflater.inflate(R.layout.lr_fragment_large_map, container, false) as MapView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        tiltOnZoomListener = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        isStarted = true
        tiltOnZoomListener?.let { listeners.add(it) }
    }

    override fun onStop() {
        tiltOnZoomListener?.let { listeners.remove(it) }
        isStarted = false
        super.onStop()
    }

    fun resetZoomTilt() {
        val mapZoom = mapZoom

        val newZoomTilt = if (mapZoom.isNaN()) {
            return
        } else if (mapZoom >= HD_ZOOM_TRANSITION_END) {
            HD_ZOOM_TILT
        } else {
            MAP_ZOOM_TILT
        }

        forcedZoom = newZoomTilt.zoom
        forcedTilt = newZoomTilt.tilt
    }

    override fun onMapReady(map: MapboxMap) {
        super.onMapReady(map)

        tiltOnZoomListener = TiltOnZoomListener(map).also {
            if (isStarted) {
                listeners.add(it)
            }
        }
    }

    private inner class TiltOnZoomListener(val map: MapboxMap) : LiveRoadsMapFragment.Listener {

        private val calculator = TiltOnZoomCalculator()

        override fun onCameraMoved(fragment: LiveRoadsMapFragment) {
        }

        override fun onFollowMeEnabledChanged(fragment: LiveRoadsMapFragment) {
            if (fragment.isFollowMeEnabled) {
                calculator.reset()
            }
        }

        override fun onIsPannedAwayFromLocationChanged(fragment: LiveRoadsMapFragment) {
        }

        override fun onZoomGestureStarted(fragment: LiveRoadsMapFragment, zoom: Double) {
            calculator.onZoomStart(zoom, map.cameraPosition.tilt)
        }

        override fun onZoomGestureEnded(fragment: LiveRoadsMapFragment, zoom: Double) {
        }

        override fun onZoomGestureProgress(fragment: LiveRoadsMapFragment, zoom: Double, sideEffects: ZoomSideEffects) {
            calculator.onZoomProgress(zoom, sideEffects)
        }

        override fun onTiltGestureStarted(fragment: LiveRoadsMapFragment, tilt: Double) {
        }

        override fun onTiltGestureEnded(fragment: LiveRoadsMapFragment, tilt: Double) {
            calculator.onTiltChangedByGesture(map.cameraPosition.zoom, tilt)
        }

        override fun onMapClick(fragment: LiveRoadsMapFragment) {
        }
    }

    private class TiltOnZoomCalculator {

        private val f = TiltZoomFunction(
                minZoom = MIN_ZOOM,
                maxZoom = MAX_ZOOM,
                minTilt = MIN_TILT,
                maxTilt = MAX_TILT
        )

        private var zoomInProgress = false

        fun reset() {
            f.reset()
            zoomInProgress = false
        }

        fun setStartValues(zoom: Double, tilt: Double) {
            f.init(zoom, tilt)
            zoomInProgress = true
        }

        fun onZoomStart(zoom: Double, tilt: Double) {
            if (!zoomInProgress) {
                setStartValues(zoom, tilt)
            }
        }

        fun onZoomProgress(zoom: Double, sideEffects: ZoomSideEffects) {
            if (zoom < MAX_ZOOM && zoom > MIN_ZOOM) {
                sideEffects.tilt = f.tiltForZoom(zoom)
            }
        }

        fun onTiltChangedByGesture(zoom: Double, tilt: Double) {
            setStartValues(zoom, tilt)
        }

    }

    private class TiltZoomFunction(val minZoom: Double, val maxZoom: Double, val minTilt: Double, val maxTilt: Double) {

        private val scaleFactor = 0.1

        private var a1 = 0.0
        private var a2 = 0.0
        private var startZoom = 0.0
        private var startTilt = 0.0

        fun reset() {
            startZoom = 0.0
            startTilt = 0.0
            a1 = 0.0
            a2 = 0.0
        }

        fun init(zoom: Double, tilt: Double) {
            startZoom = zoom
            startTilt = tilt
            a1 = (minTilt - tilt) / (scaleFactor * (minZoom - zoom)).squared()
            a2 = (maxTilt - tilt) / (scaleFactor * (maxZoom - zoom)).squared()
        }

        fun tiltForZoom(zoom: Double): Double {
            val a = if (zoom < startZoom) {
                a1
            } else if (zoom > startZoom) {
                a2
            } else {
                return startTilt
            }
            return a * (scaleFactor * (zoom - startZoom)).squared() + startTilt
        }

    }

    override fun mapZoomTilt(): ZoomTilt = MAP_ZOOM_TILT

    override fun hdZoomTilt(): ZoomTilt = HD_ZOOM_TILT

    companion object {
        @JvmField val MAP_ZOOM_TILT = ZoomTilt(zoom = 12.0, tilt = 0.0)
        @JvmField val HD_ZOOM_TILT = ZoomTilt(zoom = 19.564, tilt = 69.352)
        @JvmField val SEARCH_DISPLAYED_ZOOM_TILT = ZoomTilt(zoom = 12.0, tilt = 0.0)
        @JvmField val SEARCH_HIDDEN_ZOOM_TILT = ZoomTilt(zoom = Double.NaN, tilt = Double.NaN)

        private val MIN_TILT = Math.max(0.0, MapboxConstants.MINIMUM_TILT)
        private val MAX_TILT = Math.min(65.0, MapboxConstants.MAXIMUM_TILT)
        private val MIN_ZOOM = Math.max(8.0, MapboxConstants.MINIMUM_ZOOM.toDouble())
        private val MAX_ZOOM = Math.min(20.0, MapboxConstants.MAXIMUM_ZOOM.toDouble())
    }

}
