package com.liveroads.app

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.liveroads.common.devtools.devTools
import com.liveroads.devtools.client.monitor.PipMapStyleUrlMonitor
import com.mapbox.mapboxsdk.maps.MapView

class PipMapFragment : LiveRoadsMapFragment(
        initialCameraPosition = HD_ZOOM_TILT,
        styleUrl = "http://45.55.242.94/walid/lr/css/dayhd.json"
) {

    init {
        isStreetNameVisible = true
    }

    private var zoomTilt = HD_ZOOM_TILT
    private var mapZoomTiltAnimator: MapZoomTiltAnimator? = null

    override val styleUrlMonitor = PipMapStyleUrlMonitor(devTools)
    override val myLocationAboveStreetNameMarginBottomResId = R.dimen.lr_my_location_margin_bottom_pip_map

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateForcedZoomTilt()
    }

    override fun onCreateMapView(inflater: LayoutInflater, container: FrameLayout, savedInstanceState: Bundle?): MapView {
        return inflater.inflate(R.layout.lr_fragment_pip_map, container, false) as MapView
    }

    override fun onStart() {
        super.onStart()
        updateForcedZoomTilt()
    }

    override fun onStop() {
        mapZoomTiltAnimator?.cancel()
        mapZoomTiltAnimator = null
        super.onStop()
    }

    private fun updateForcedZoomTilt() {
        forcedZoom = zoomTilt.zoom
        forcedTilt = zoomTilt.tilt
    }

    fun animateSetZoomTilt(newZoomTilt: ZoomTilt) {
        val oldZoomTilt = zoomTilt
        zoomTilt = newZoomTilt

        if (mapZoomTiltAnimator != null || newZoomTilt == oldZoomTilt) {
            return
        }

        val animator = MapZoomTiltAnimator()

        animator.addUpdateListener {
            val pct = it.animatedFraction.toDouble()
            forcedZoom = oldZoomTilt.zoom + (pct * (newZoomTilt.zoom - oldZoomTilt.zoom))
            forcedTilt = oldZoomTilt.tilt + (pct * (newZoomTilt.tilt - oldZoomTilt.tilt))
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(a: Animator) {
                mapZoomTiltAnimator = null
                animateSetZoomTilt(zoomTilt)
            }

            override fun onAnimationCancel(a: Animator) {
            }

            override fun onAnimationStart(a: Animator) {
            }

            override fun onAnimationRepeat(a: Animator) {
            }
        })

        animator.start()
    }

    private class MapZoomTiltAnimator : ValueAnimator() {

        init {
            setFloatValues(0f, 1f)
            interpolator = LinearInterpolator()
            duration = 1500L
        }

    }

    companion object {
        @JvmField val MAP_ZOOM_TILT = ZoomTilt(zoom = 13.0, tilt = 0.0)
        @JvmField val HD_ZOOM_TILT = ZoomTilt(zoom = 19.242, tilt = 68.529)
    }

    override fun mapZoomTilt(): ZoomTilt {
        return MAP_ZOOM_TILT
    }

    override fun hdZoomTilt(): ZoomTilt {
        return HD_ZOOM_TILT
    }

}
