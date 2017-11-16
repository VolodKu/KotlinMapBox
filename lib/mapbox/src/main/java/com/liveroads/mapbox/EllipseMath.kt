package com.liveroads.mapbox

import android.graphics.PointF

/**
 * Performs math with an ellipse for calculating camera position when the map is tilted.
 *
 * The coordinate system assumes that (0,0) is the top-left corner and the increasing X values moves to the right and
 * increasing Y values moves down.
 *
 * Note that internally the Y values are often negated since in a cartesian plane increasing Y values move up, not down,
 * so the negation corrects for this (however this fact is never exposed externally from this class).
 */
class EllipseMath {

    private val center = PointF()
    private val radii = PointF()

    /**
     * Set the coordinates of the center of the ellipse.
     */
    fun setCenterCoordinates(cx: Float, cy: Float) {
        center.x = cx
        center.y = -cy
    }

    /**
     * Get the angle, in radians, of the given point relative to the center of the map.
     * If the given point is directly to the right of center then 0 is returned.
     * If the given point is directly above the center then π/2 is returned.
     * If the given point is directly to the left of center then π is returned.
     * If the given point is directly below center then 3π/2 is returned.
     *
     * @return the angle in radians in the range 0..2π (0 included, 2π excluded)
     */
    fun angleTo(x: Float, y: Float): Double {
        val xTranslated = x - center.x
        val yTranslated = ((-y) - center.y)
        val angle = Math.atan2(yTranslated.toDouble(), xTranslated.toDouble())
        return if (angle > 0) {
            angle
        } else {
            (2 * Math.PI) + angle
        }
    }

    /**
     * Set the horizontal and vertical radii to create an ellipse.
     *
     * The center coordinates that were previously set will be used as the ellipse's center.
     *
     * The tilt must be specified in radians between 0..π/2, inclusive.
     *
     * The ellipse will have the following properties:
     * 1. The ellipse will pass through the given point x,y
     * 2. The ellipse will be projected as a circle onto a plane tilted at the given angle about the line y=cx (the
     * center line through the circle).
     *
     * See https://math.stackexchange.com/a/2389317/470890 for derivation
     */
    fun setRadiiForProjectedPointOfTiltedCircle(x: Float, y: Float, tilt: Double) {
        val xWithCenterOffset = (x - center.x).toDouble()
        val xWithCenterOffsetSquared = xWithCenterOffset * xWithCenterOffset
        val yWithCenterOffset = ((-y) - center.y).toDouble()
        val yWithCenterOffsetSquared = yWithCenterOffset * yWithCenterOffset
        val cosTilt = Math.cos(tilt)
        val cosTiltSquared = cosTilt * cosTilt

        val numerator = yWithCenterOffsetSquared + (xWithCenterOffsetSquared * cosTiltSquared)
        val rx = Math.sqrt(numerator / cosTiltSquared)
        val ry = Math.sqrt(numerator)

        radii.x = rx.toFloat()
        radii.y = ry.toFloat()
    }

    /**
     * Same as [setRadiiForProjectedPointOfTiltedCircle] but approximated with a circle instead of an ellipse.
     * Delete this method once setRadiiForProjectedPointOfTiltedCircle() is perfected.
     */
    fun setRadiiForPointOfCircle(x: Float, y: Float) {
        val deltaX = (x.toDouble() - center.x.toDouble())
        val deltaY = ((-y).toDouble() - center.y.toDouble())
        val radius = Math.hypot(deltaX, deltaY)
        radii.x = radius.toFloat()
        radii.y = radius.toFloat()
    }

    fun getPointByAngle(angle: Double, point: PointF) {
        val x = center.x + (radii.x * Math.cos(angle))
        val y = center.y + (radii.y * Math.sin(angle))
        point.x = x.toFloat()
        point.y = (-y).toFloat()
    }

}
