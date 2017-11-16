package com.liveroads.app.adviser

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log

class SpeedAverageCalculator : Handler(HandlerThread("speed-calculator").apply { start() }.looper) {
    private var curTotal: Double = 0.0
    private var curCount: Long = 0

    @Volatile var speed: Double = 0.0
        private set

    init {
        sendEmptyMessageDelayed(WHAT_CALC_AVERAGE, 2000L)
    }

    fun addSpeed(s: Double) {
        if (hasMessages(WHAT_ADD_SPEED)) { return }
        sendMessageDelayed(Message.obtain(this, WHAT_ADD_SPEED).apply { obj = s }, 1000L)
    }
    companion object {
        val WHAT_ADD_SPEED = 1
        val WHAT_CALC_AVERAGE = 2
    }

    override fun handleMessage(msg: Message?) {
        msg?.let {
            when(it.what) {
                WHAT_ADD_SPEED -> addSpeed(msg)
                WHAT_CALC_AVERAGE -> updateAverage()
            }
        }
    }

    private fun addSpeed(m: Message) {
        m.obj.let {
            curTotal += it as Double
            curCount += 1
        }
    }

    private fun updateAverage() {
        speed = curTotal/curCount
        curTotal = 0.0
        curCount = 0
        sendEmptyMessageDelayed(WHAT_CALC_AVERAGE, 2000L)
    }

}
