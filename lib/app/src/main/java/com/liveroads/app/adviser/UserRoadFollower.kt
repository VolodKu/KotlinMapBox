package com.liveroads.app.adviser

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.MainThread
import com.liveroads.common.executors.Executors
import com.liveroads.location.StreetNameCalculateTask
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * all fields are in SI: meters, seconds
 * also all interfaces are expecting SI params
 *
 */

object UserRoadFollower {
    private var listeners: MutableList<RoadFollowerListener> = CopyOnWriteArrayList()

    private val speedAverageCalculator = SpeedAverageCalculator()
    private val distanceCalculator = DistanceCalculator()
    private @Volatile var wholeRouteDistance: Long = 0
    private @Volatile var nextTurnDirection: TURN_DIRECTION = TURN_DIRECTION.UNSUPPORTED
    private @Volatile var nextStreetName: String = ""

    private val callbackDispatcherHandler = DispatcherHandler()


    init {
        callbackDispatcherHandler.start()
    }

    fun updateRouteDistance(dist: Long) {
        if (dist != wholeRouteDistance) {
            wholeRouteDistance = dist
        }
    }

    fun updateCurrentLocation(long: Double, lat: Double) {
        distanceCalculator.updateStartPoint(lat, long)
    }

    /**
     * @param modifier - modifier of `StepManeuver` from mapbox.
     *                  something like turn direction
     *
     */
    fun updateNextTurnCoordinates(long: Double, lat: Double, modifier: String?) {
        distanceCalculator.updateEndPoint(lat, long)
        nextTurnDirection = TURN_DIRECTION.from(modifier)
        val r = StreetNameCalculateTask.RequestInfo(lat.toString(), long.toString())
        StreetNameCalculateTask(r, StreetNameCallback())
                .executeOnExecutor(Executors.NORMAL_PRIORITY_IMMEDIATE)
    }

    fun updateSpeed(speed: Double) {
        speedAverageCalculator.addSpeed(speed)
    }

    @MainThread
    fun addListener(listener: RoadFollowerListener) {
        listeners.add(listener)
    }

    @MainThread
    fun removeListener(listener: RoadFollowerListener) {
        for (i in listeners.size - 1 downTo 0) {
            if (listeners[i] === listener) {
                listeners.removeAt(i)
                break
            }
        }
    }

    data class NextTurnInfo(val dist: Long, val time: Long,
                            val streetName: String, val direction: TURN_DIRECTION,
                            val speedKmH: Double)

    data class RouteInfo(val dist: Long, val time: Long)

    enum class TURN_DIRECTION {
        LEFT, RIGHT, UNSUPPORTED;

        companion object {
            private val validModifiers: HashMap<String, TURN_DIRECTION> = hashMapOf(
                    Pair("sharp right", RIGHT),
                    Pair("right", RIGHT),
                    Pair("slight right", RIGHT),
                    Pair("slight left", LEFT),
                    Pair("left", LEFT),
                    Pair("sharp left", LEFT))

            fun isValid(modifier: String?): Boolean = validModifiers.containsKey(modifier)

            fun from(modifier: String?): TURN_DIRECTION =
                    validModifiers[modifier]
                            ?: UNSUPPORTED
        }
    }

    internal class DispatcherHandler : Handler(Looper.getMainLooper()) {
        private val WHAT = 1
        init {
        }

        fun start() {
            removeCallbacksAndMessages(null)
            sendEmptyMessageDelayed(WHAT, 1000L)
        }

        override fun handleMessage(msg: Message?) {
            if (listeners.size > 0) {
                notifyAboutRoute()
                notifyAboutNextTurn()

            }
            sendEmptyMessageDelayed(WHAT, 1000L)
        }

        private fun notifyAboutRoute() {
            val speedAverage = speedAverageCalculator.speed
            listeners.forEach {
                val u = RouteInfo(wholeRouteDistance, calcTime(speedAverage, wholeRouteDistance))
                it.onRouteInfoChanged(u)
            }
        }

        private fun notifyAboutNextTurn() {
            val nextTurnDistance = distanceCalculator.distance
            val speedAverage = speedAverageCalculator.speed

            val nextTurnTravelTime = if (speedAverage > 0 && nextTurnDistance > 0) {
                (nextTurnDistance / speedAverage).toLong()
            } else {
                -1
            }
            val speedKmH: Double = speedAverage * 3.6
            listeners.forEach {
                val u = NextTurnInfo(nextTurnDistance, nextTurnTravelTime,
                        nextStreetName, nextTurnDirection, speedKmH)
                it.onNextTurnInfoChanged(u)
            }

        }

        private fun calcTime(speed: Double, dist: Long): Long {
            return if (speed > 0 && dist > 0) {
                (dist / speed).toLong()
            } else {
                0L
            }
        }
    }

    class StreetNameCallback : StreetNameCalculateTask.Callback {
        override fun onStreetNameCalculateTaskComplete(task: StreetNameCalculateTask, result: StreetNameCalculateTask.Result) {
            if (result is StreetNameCalculateTask.Result.Success) {
                nextStreetName = result.streetName ?: ""
            }
        }

    }
}
