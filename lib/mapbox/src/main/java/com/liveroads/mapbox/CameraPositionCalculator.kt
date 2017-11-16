package com.liveroads.mapbox

import android.graphics.PointF
import com.mapbox.mapboxsdk.maps.MapboxMap

/**
 * Helper class to calculate camera positions where the desired location is offset from the center of the map.
 *
 * Mapbox camera positions cause the camera to position the center of the map at a specific location; however, if you
 * want a position to instead be displayed somewhere other than the center then you must calculate what center position
 * will cause the location to be placed at a desired position in the screen.  This calculation can be complex because
 * it must take into account tilt and zoom, which cause the offset to be non-linear.
 *
 * This class is NOT thread-safe; concurrent use from multiple threads has undefined behaviour.  Be sure to never use
 * an instance of this class concurrently in multiple threads.  Normally, an instance of this class is used exclusively
 * by the main event thread.
 */
class CameraPositionCalculator(val map: MapboxMap) {

    private var calculatedResult = CalculatedResult(0.0, 0.0, 0.0, 0.0)
    private val ellipse = EllipseMath()
    private val tmpPointF = PointF()

    fun calculatePosition(x: Float, y: Float, latitude: Double, longitude: Double, altitude: Double, bearing: Double)
            : CalculatedResult {
        return if (x.isNaN() && x.isNaN()) {
            calculatedResult.update(latitude, longitude, altitude, bearing)
        } else {
            val effectiveX = if (x.isNaN()) (map.uiSettings.width / 2f) else x
            val effectiveY = if (y.isNaN()) (map.uiSettings.height / 2f) else y
            _calculatePosition(effectiveX, effectiveY, latitude, longitude, altitude, bearing)
        }
    }

    private fun _calculatePosition(x: Float, y: Float, latitude: Double, longitude: Double, altitude: Double,
            bearing: Double): CalculatedResult {
        val width = map.uiSettings.width
        val height = map.uiSettings.height
        ellipse.setCenterCoordinates(width / 2f, height / 2f)

        //val tiltDegrees = map.cameraPosition.tilt
        //val tiltRadians = Math.toRadians(tiltDegrees)
        //ellipse.setRadiiForProjectedPointOfTiltedCircle(x, y, tiltRadians)
        ellipse.setRadiiForPointOfCircle(x, y)

        val curBearingDegrees = map.cameraPosition.bearing
        val curBearingRadians = Math.toRadians(curBearingDegrees)
        val newBearingDegrees = bearing
        val newBearingRadians = Math.toRadians(newBearingDegrees)

        val bearingDeltaRadians = newBearingRadians - curBearingRadians
        val xyAngleRadians = ellipse.angleTo(x, y)

        val newAngleRadians = xyAngleRadians - bearingDeltaRadians
        val point = tmpPointF
        ellipse.getPointByAngle(newAngleRadians, point)

        val curXYLatLng = map.projection.fromScreenLocation(point)
                ?: return calculatedResult.update(latitude, longitude, altitude, bearing)

        val centerLatitude = map.cameraPosition.target.latitude
        val centerLongitude = map.cameraPosition.target.longitude
        val centerAltitude = map.cameraPosition.target.altitude

        val curXYLatitude = curXYLatLng.latitude
        val curXYLongitude = curXYLatLng.longitude
        val curXYAltitude = curXYLatLng.altitude

        val latitudeOffset = curXYLatitude - centerLatitude
        val longitudeOffset = curXYLongitude - centerLongitude
        val altitudeOffset = curXYAltitude - centerAltitude

        val newLatitude = latitude - latitudeOffset
        val newLongitude = longitude - longitudeOffset
        val newAltitude = altitude - altitudeOffset

        return calculatedResult.update(newLatitude, newLongitude, newAltitude, bearing)
    }

    data class CalculatedResult(
            var latitude: Double,
            var longitude: Double,
            var altitude: Double,
            var bearing: Double
    ) {

        fun update(latitude: Double, longitude: Double, altitude: Double, bearing: Double): CalculatedResult {
            this.latitude = latitude
            this.longitude = longitude
            this.altitude = altitude
            this.bearing = bearing
            return this
        }


    }

}
