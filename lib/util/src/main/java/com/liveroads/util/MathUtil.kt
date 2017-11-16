package com.liveroads.util

/**
 * Clamps a float value into a certain range.
 *
 * If the float is less than the given minimum value then that minimum value is returned.
 * If the float is greater than the given maximum value then that maximum value is returned.
 * Otherwise, the given float is returned.
 * If the given float is [Float.NaN] then [Float.NaN] is returned.
 */
fun Float.clamp(min: Float, max: Float): Float {
    if (min.isNaN()) {
        throw IllegalArgumentException("min.isNaN()")
    } else if (max.isNaN()) {
        throw IllegalArgumentException("max.isNaN()")
    } else if (min > max) {
        throw IllegalArgumentException("min>max: min=$min max=$max")
    } else if (isNaN()) {
        return Float.NaN
    } else if (this < min) {
        return min
    } else if (this > max) {
        return max
    } else {
        return this
    }
}

/**
 * Clamps a double value into a certain range.
 *
 * If the double is less than the given minimum value then that minimum value is returned.
 * If the double is greater than the given maximum value then that maximum value is returned.
 * Otherwise, the given double is returned.
 * If the given double is [Double.NaN] then [Double.NaN] is returned.
 */
fun Double.clamp(min: Double, max: Double): Double {
    if (min.isNaN()) {
        throw IllegalArgumentException("min.isNaN()")
    } else if (max.isNaN()) {
        throw IllegalArgumentException("max.isNaN()")
    } else if (min > max) {
        throw IllegalArgumentException("min>max: min=$min max=$max")
    } else if (isNaN()) {
        return Double.NaN
    } else if (this < min) {
        return min
    } else if (this > max) {
        return max
    } else {
        return this
    }
}

/**
 * Clamps an integer value into a certain range.
 *
 * If the integer is less than the given minimum value then that minimum value is returned.
 * If the integer is greater than the given maximum value then that maximum value is returned.
 * Otherwise, the given integer is returned.
 */
fun Int.clamp(min: Int, max: Int): Int {
    if (min > max) {
        throw IllegalArgumentException("min>max: min=$min max=$max")
    } else if (this < min) {
        return min
    } else if (this > max) {
        return max
    } else {
        return this
    }
}

fun Double.squared() = (this * this)
