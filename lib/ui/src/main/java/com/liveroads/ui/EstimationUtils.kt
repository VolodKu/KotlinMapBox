package com.liveroads.ui

object EstimationUtils {

    val UNKNOWN_TIME = "??h ??m"
    val UNKNOWN_DISTANCE = "?? m"
    val HOUR_SEC = 3600L

    fun convertTime(timeSec: Long): String {
        if (timeSec == -1L) {
            return UNKNOWN_TIME
        }
        return if (timeSec > HOUR_SEC) {
            val m = timeSec/60
            String.format("%dh %02dm", m/60, m%60)
        } else {
            String.format("%dm %02ds", timeSec/60, timeSec%60)
        }
    }

    fun convertDistance(distMeters: Long): String {
        if (distMeters == -1L) {
            return UNKNOWN_DISTANCE
        }
        return if (distMeters < 1000) {
            String.format("%d m", distMeters)
        } else {
            String.format("%d km", distMeters/1000)
        }
    }

}
