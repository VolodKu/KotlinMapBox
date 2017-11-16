package com.liveroads.app.adviser

import android.content.res.Resources
import com.liveroads.app.R
import com.liveroads.app.adviser.tts.TTSReader

/**
 * relying that updates coming in every 1 second.
 * if things will change then store last result
 * and pull it with own handler as slow/fast as needed
 */
object VoiceAdviser : RoadFollowerListener {

    private val NOTIFY_DISTANCE = "Turn on %s in %d %s"

    private lateinit var resources: Resources
    private lateinit var nextTurnNarrator: TimeToNextTurnNarrator

    private var isStarted = false
        private set

    fun init(ntn: TimeToNextTurnNarrator, resources: Resources) {
        nextTurnNarrator = ntn
        this.resources = resources
    }

    fun start() {
        if (isStarted) { return }
        UserRoadFollower.addListener(this)
    }

    fun stop() {
        if (!isStarted) { return }
        UserRoadFollower.removeListener(this)
    }

    private fun notify(dist: Long, time: Long, streetName: String) {

        when(dist) {
            2000L -> {
                notifyNextTurn(2000, streetName)
            }
            1000L -> {
                notifyNextTurn(1000, streetName)
            }
            500L -> {
                notifyNextTurn(500, streetName)
            }
        }

        if (dist <= 500) {
            nextTurnNarrator.startIfNot(time, streetName)
        } else {
            nextTurnNarrator.stop()
        }
    }

    private fun notifyNextTurn(dist: Int, street: String) {
        val c = if (dist >= 1000) dist/1000 else dist
        val s = if (dist >= 1000) (resources.getQuantityString(R.plurals.kilometers, c))
                else (resources.getQuantityString(R.plurals.meters, c))

        val m = NOTIFY_DISTANCE.format(street, c, s)
        TTSReader.sayNow(m)
    }

    override fun onNextTurnInfoChanged(nt: UserRoadFollower.NextTurnInfo) {
        if (nt.dist >= 0 && nt.time >= 0) {
            notify(nt.dist, nt.time, nt.streetName)
        }
    }

    override fun onRouteInfoChanged(ri: UserRoadFollower.RouteInfo) {
        //pass
    }

}
