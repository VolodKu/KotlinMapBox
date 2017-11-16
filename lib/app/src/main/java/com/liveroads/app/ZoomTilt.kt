package com.liveroads.app

data class ZoomTilt(val zoom: Double, val tilt: Double) {
    fun isSame(anotherZoom: Double?, anotherTilt: Double?): Boolean =
            zoom == anotherZoom && tilt == anotherTilt
}
