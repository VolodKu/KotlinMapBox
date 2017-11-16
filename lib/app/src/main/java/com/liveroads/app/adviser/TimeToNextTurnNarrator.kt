package com.liveroads.app.adviser

import android.content.res.Resources
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.liveroads.app.R
import com.liveroads.app.adviser.tts.TTSReader


// @NotThreadSafe
class TimeToNextTurnNarrator(val resources: Resources)
    : Handler(Looper.getMainLooper()), RoadFollowerListener
{
    private @Volatile var secondsLeft: Long = 0
    private @Volatile var streetName: String = ""
    private @Volatile var speed: Double = 0.0

    private var isStarted = false

    companion object {
        private val SPEECH_DELAY = 6500L   // 5 sec delay + 1.5 sec for reading
        private val BEEP_DELAY = 1000L
        private val TIMER_DELAY = 500L

        private val WHAT_TIMER = 1
        private val WHAT_SPEECH = 2
        private val WHAT_BEEP = 3

        private val NEXT_TURN_MSG = "Turn on %s in %d %s"
        private val TURN_NOW_MSG = "Turn now"
    }

    fun startIfNot(initialTime: Long, initialStreetName: String) {
        if (isStarted) { return }

        isStarted = true
        secondsLeft = initialTime
        streetName = initialStreetName

        if (shouldStartBeeping(initialTime)) {
            startBeeping()
        }
        startTimer()

        UserRoadFollower.addListener(this)
    }

    fun stop() {
        if (!isStarted) { return }

        isStarted = false

        stopEverything()
        UserRoadFollower.removeListener(this)
    }

    private fun shouldStartBeeping(nextTurnTime: Long): Boolean = nextTurnTime in 0..3
    private fun shouldStopBeeping(nextTurnTime: Long): Boolean = nextTurnTime in 0..1

    private fun startTimer() = sendMessageDelayed(Message.obtain(this, WHAT_TIMER), TIMER_DELAY)
    private fun stopTimer() = removeMessages(WHAT_TIMER)

    private fun startSpeaking() {
        Log.d("","start speaking")
        sayTimeLeft()
        removeMessages(WHAT_SPEECH)
        sendMessageDelayed(Message.obtain(this, WHAT_SPEECH), SPEECH_DELAY)
    }

    private fun stopSpeaking() {
        removeMessages(WHAT_SPEECH)
        TTSReader.stop()
    }

    private fun startBeeping() {
        makeBeep()
        removeMessages(WHAT_BEEP)
        sendMessageDelayed(Message.obtain(this, WHAT_BEEP), BEEP_DELAY)
    }
    private fun stopBeeping() = removeMessages(WHAT_BEEP)

    private fun stopEverything() = removeCallbacksAndMessages(null)

    private fun rescheduleThings(nextTurnTime: Long) {
        if (shouldStartBeeping(nextTurnTime)) {
            stopSpeaking()
            startBeeping()
        } else if (canSpeak() && !hasMessages(WHAT_SPEECH)) {
            startSpeaking()
        }
    }

    override fun handleMessage(msg: Message) {
        when(msg.what) {
            WHAT_TIMER -> {
                rescheduleThings(secondsLeft)
                startTimer()
            }
            WHAT_SPEECH -> {
                if (canSpeak()) {
                    startSpeaking()
                }
            }
            WHAT_BEEP -> {
                if (shouldStopBeeping(secondsLeft)) {
                    stopBeeping()
                    sayTurnNow()
                } else {
                    stopSpeaking()
                    startBeeping()
                }
            }
        }
    }

    private fun sayTimeLeft() {
        val m = NEXT_TURN_MSG.format(streetName, secondsLeft,
                resources.getQuantityString(R.plurals.seconds, secondsLeft.toInt()))
        TTSReader.sayNow(m)
    }

    private fun sayTurnNow() {
        TTSReader.sayNow(TURN_NOW_MSG)
    }

    private fun makeBeep() {
        val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneG.startTone(ToneGenerator.TONE_DTMF_S, 200)
    }

    // RoadFollowerListener

    override fun onNextTurnInfoChanged(nt: UserRoadFollower.NextTurnInfo) {
        secondsLeft = nt.time
        streetName = nt.streetName
        speed = nt.speedKmH
    }

    override fun onRouteInfoChanged(ri: UserRoadFollower.RouteInfo) {
        //pass
    }

    private fun canSpeak(): Boolean {
        return (speed > 120 && secondsLeft in 1..30)
                || (speed in 80..120 && secondsLeft in 1..25)
                || (speed in 50..79 && secondsLeft in 1..15)
                || (speed in 1..49 && secondsLeft in 1..10)
    }
}
