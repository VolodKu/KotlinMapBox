package com.liveroads.app

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.liveroads.mapbox.CameraPositionCalculator
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

class CameraPositionAnimator(val map: MapboxMap)
    : ValueAnimator(), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    var isInitialized = false
        private set

    var latitude = Double.NaN
    var longitude = Double.NaN
    var altitude = Double.NaN
    var bearing = Double.NaN
    var zoom = Double.NaN
    var tilt = Double.NaN
    var myLocationX = Float.NaN
    var myLocationY = Float.NaN

    private var startLatitude = Double.NaN
    private var startLongitude = Double.NaN
    private var startAltitude = Double.NaN
    private var startBearing = Double.NaN
    private var startZoom = Double.NaN
    private var startTilt = Double.NaN
    private var startMyLocationX = Float.NaN
    private var startMyLocationY = Float.NaN

    private var lastLatitude = Double.NaN
    private var lastLongitude = Double.NaN
    private var lastAltitude = Double.NaN
    private var lastBearing = Double.NaN
    private var lastZoom = Double.NaN
    private var lastTilt = Double.NaN
    private var lastMyLocationX = Float.NaN
    private var lastMyLocationY = Float.NaN

    private val cameraPositionCalculator = CameraPositionCalculator(map)

    init {
        setFloatValues(0f, 1f)
        interpolator = LinearInterpolator()
        duration = 300
        addListener(this)
        addUpdateListener(this)
    }

    override fun onAnimationUpdate(animator: ValueAnimator) {
        updateMap(animatedFraction)
    }

    override fun onAnimationRepeat(animator: Animator) {
    }

    override fun onAnimationEnd(animator: Animator) {
    }

    override fun onAnimationCancel(animator: Animator) {
    }

    override fun onAnimationStart(animator: Animator) {
        captureStartValues()
    }

    fun updateLastValuesFromMap(myLocation: LatLng) {
        lastLatitude = map.cameraPosition.target.latitude
        lastLongitude = map.cameraPosition.target.longitude
        lastAltitude = map.cameraPosition.target.altitude
        lastBearing = map.cameraPosition.bearing
        lastZoom = map.cameraPosition.zoom
        lastTilt = map.cameraPosition.tilt

        val point = map.projection.toScreenLocation(myLocation)
        if (point != null) {
            lastMyLocationX = point.x
            lastMyLocationY = point.y
        }
    }

    fun captureStartValues() {
        startLatitude = lastLatitude
        startLongitude = lastLongitude
        startAltitude = lastAltitude
        startBearing = lastBearing
        startZoom = lastZoom
        startTilt = lastTilt
        startMyLocationX = lastMyLocationX
        startMyLocationY = lastMyLocationY
        isInitialized = true
    }

    fun updateMap(pct: Float) {
        val newLatitude = calculateAnimatedValue(pct, startLatitude, latitude)
        val newLongitude = calculateAnimatedValue(pct, startLongitude, longitude)
        val newAltitude = calculateAnimatedValue(pct, startAltitude, altitude)
        val newBearing = calculateAnimatedDegrees(pct, startBearing, bearing)
        val newZoom = calculateAnimatedValue(pct, startZoom, zoom)
        val newTilt = calculateAnimatedValue(pct, startTilt, tilt)
        val newMyLocationX = calculateAnimatedValue(pct, startMyLocationX, myLocationX)
        val newMyLocationY = calculateAnimatedValue(pct, startMyLocationY, myLocationY)
        updateMap(newLatitude, newLongitude, newAltitude, newBearing, newZoom, newTilt, newMyLocationX, newMyLocationY)
    }

    fun updateMap() {
        updateMap(latitude, longitude, altitude, bearing, zoom, tilt, myLocationX, myLocationY)
    }

    fun updateMap(newLatitude: Double, newLongitude: Double, newAltitude: Double, newBearing: Double,
            newZoom: Double, newTilt: Double, newMyLocationX: Float, newMyLocationY: Float) {
        lastLatitude = newLatitude
        lastLongitude = newLongitude
        lastAltitude = newAltitude
        lastBearing = newBearing
        lastZoom = newZoom
        lastTilt = newTilt
        lastMyLocationX = newMyLocationX
        lastMyLocationY = newMyLocationY

        val calculatedPosition = cameraPositionCalculator.calculatePosition(
                x = newMyLocationX,
                y = newMyLocationY,
                latitude = newLatitude,
                longitude = newLongitude,
                altitude = newAltitude,
                bearing = newBearing)

        val newCameraPosition = CameraPosition.Builder().let {
            if (isValidPosition(calculatedPosition)) {
                it.target(LatLng(calculatedPosition.latitude, calculatedPosition.longitude, calculatedPosition.altitude))
            }
            it.bearing(calculatedPosition.bearing)
            //it.zoom(newZoom)
            //it.tilt(newTilt)
            it.build()
        }

        map.cameraPosition = newCameraPosition
    }

    fun calculatePosition() = cameraPositionCalculator.calculatePosition(
            x = myLocationX,
            y = myLocationY,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            bearing = bearing)

}

private fun isValidPosition(calculatedPosition: CameraPositionCalculator.CalculatedResult): Boolean {
    return calculatedPosition.latitude >= -90.0 && calculatedPosition.latitude <= 90.0
}

private fun calculateAnimatedValue(pct: Float, start: Double, end: Double): Double {
    if (start.isNaN() || end.isNaN()) {
        throw IllegalArgumentException("start==$start end=$end")
    }
    val difference = end - start
    val offset = difference * pct
    return start + offset
}

private fun calculateAnimatedValue(pct: Float, start: Float, end: Float): Float {
    if (start.isNaN() || end.isNaN()) {
        throw IllegalArgumentException("start==$start end=$end")
    }
    val difference = end - start
    val offset = difference * pct
    return start + offset
}

private fun calculateAnimatedDegrees(pct: Float, start: Double, end: Double): Double {
    if (start.isNaN() || end.isNaN()) {
        throw IllegalArgumentException("start==$start end=$end")
    }

    val difference = end - start
    val absDifference = Math.abs(difference)

    return if (absDifference <= 180) {
        val offset = difference * pct
        start + offset
    } else {
        if (end >= start) {
            val distance = start + 360 - end
            val progress = distance * pct
            if (start >= progress) {
                start - progress
            } else {
                start + 360 - progress
            }
        } else {
            val distance = end + 360 - start
            val progress = distance * pct
            if (start + progress < 360) {
                start + progress
            } else {
                start - 360 + progress
            }
        }
    }
}
