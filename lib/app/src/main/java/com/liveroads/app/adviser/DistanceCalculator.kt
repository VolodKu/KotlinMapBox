package com.liveroads.app.adviser

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.mapbox.services.api.utils.turf.TurfConstants
import com.mapbox.services.api.utils.turf.TurfMeasurement
import com.mapbox.services.commons.models.Position

class DistanceCalculator : Handler(HandlerThread("distance-calculator").apply { start() }.looper) {

    private var startLat = 0.0
    private var startLong = 0.0
    private var endLat = 0.0
    private var endLong = 0.0

    @Volatile var distance = 0L
        private set

    companion object {
        val WHAT = 1
    }

    init {
        sendEmptyMessageDelayed(WHAT, 1000L)
    }

    fun updateStartPoint(lat: Double, long: Double) {
        startLat = lat
        startLong = long
    }

    fun updateEndPoint(lat: Double, long: Double) {
        endLat = lat
        endLong = long
    }

    override fun handleMessage(msg: Message?) {
        msg?.what.let {
            if (it == WHAT) {
                updateDistance()
            }
        }
    }

    private fun updateDistance() {
        distance = TurfMeasurement.distance(
                Position.fromCoordinates(startLong, startLat),
                Position.fromCoordinates(endLong, endLat),
                TurfConstants.UNIT_METERS).toLong()

        sendEmptyMessageDelayed(WHAT, 1000L)
    }

}
