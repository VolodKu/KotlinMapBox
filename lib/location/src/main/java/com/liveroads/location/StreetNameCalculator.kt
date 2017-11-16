package com.liveroads.location

import android.location.Location
import android.os.Handler
import android.os.SystemClock
import android.support.annotation.MainThread
import android.util.LruCache
import com.liveroads.common.executors.Executors
import com.liveroads.common.log.obtainLogger
import java.util.concurrent.TimeUnit

internal class StreetNameCalculator(var ttl: Long, var ttlUnit: TimeUnit) {

    var listener: Listener? = null

    var streetName: String? = null
        private set
    var maxSpeed: Double? = null
        private set

    private val logger = obtainLogger()
    private lateinit var mainHandler: Handler
    private var streetNameCalculateTask: StreetNameCalculateTask? = null
    private var nextUpdateTime = SystemClock.uptimeMillis()
    private var latitude = Double.NaN
    private var longitude = Double.NaN
    private var locationChanged = false
    private val cache =
            LruCache<StreetNameCalculateTask.RequestInfo,
                    StreetNameCalculateTask.Result.Success>(100)

    @MainThread
    fun onCreate() {
        mainHandler = Handler()
    }

    @MainThread
    fun onDestroy() {
        streetNameCalculateTask?.cancel(true)
        streetNameCalculateTask = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    @MainThread
    fun onLocationChanged(location: Location, forceStreetNameCalculation: Boolean = false) {
        latitude = location.latitude
        longitude = location.longitude
        locationChanged = true
        startStreetNameCalculateTask(forceStreetNameCalculation)
    }

    private fun startStreetNameCalculateTask(force: Boolean) {
        if (force) {
            streetNameCalculateTask?.cancel(true)
            streetNameCalculateTask = null
        }

        if (streetNameCalculateTask == null && (force || SystemClock.uptimeMillis() >= nextUpdateTime)) {
            val requestInfo = StreetNameCalculateTask.RequestInfo.forLatitudeLongitude(latitude, longitude)
            if (requestInfo != null) {
                val cachedResponse = cache.get(requestInfo)
                if (cachedResponse != null) {
                    mainHandler.post(
                            StreetInfoChangedRunnable(
                                    requestInfo,
                                    cachedResponse.streetName,
                                    cachedResponse.maxSpeed))
                } else {
                    streetNameCalculateTask = StreetNameCalculateTask(requestInfo, streetNameCalculateTaskCallback).apply {
                        executeOnExecutor(Executors.NORMAL_PRIORITY_IMMEDIATE)
                    }
                }
            }
            mainHandler.removeCallbacks(startStreetNameCalculateTaskRunnable)
            locationChanged = false
        } else {
            val delay = ttlUnit.toMillis(ttl)
            mainHandler.postDelayed(startStreetNameCalculateTaskRunnable, delay)
        }
    }

    private fun onStreetNameCalculateTaskComplete(task: StreetNameCalculateTask, result: StreetNameCalculateTask.Result) {
        if (task !== streetNameCalculateTask) {
            throw IllegalArgumentException("incorrect task: $task !== $streetNameCalculateTask")
        }
        streetNameCalculateTask = null

        if (result is StreetNameCalculateTask.Result.Success) {
            cache.put(task.requestInfo, result)
            onStreetInfoChanged(task.requestInfo, result.streetName, maxSpeed)
        } else {
            nextUpdateTime = SystemClock.uptimeMillis() + ttlUnit.toMillis(ttl)
        }
    }

    private fun onStreetInfoChanged(
            requestInfo: StreetNameCalculateTask.RequestInfo,
            newStreetName: String?,
            newMaxSpeed: Double?)
    {
        //logger.logLifecycle("onStreetInfoChanged() location=%s streetName=%s", requestInfo, newStreetName)
        streetName = newStreetName
        maxSpeed = newMaxSpeed
        listener?.onStreetInfoChanged(this, newStreetName, newMaxSpeed)
        nextUpdateTime = SystemClock.uptimeMillis() + ttlUnit.toMillis(ttl)
    }

    private val streetNameCalculateTaskCallback = object : StreetNameCalculateTask.Callback {
        override fun onStreetNameCalculateTaskComplete(task: StreetNameCalculateTask,
                result: StreetNameCalculateTask.Result) {
            this@StreetNameCalculator.onStreetNameCalculateTaskComplete(task, result)
        }
    }

    private val startStreetNameCalculateTaskRunnable = Runnable {
        if (locationChanged) {
            startStreetNameCalculateTask(false)
        }
    }

    private inner class StreetInfoChangedRunnable(
            val requestInfo: StreetNameCalculateTask.RequestInfo,
            val streetName: String?, val maxSpeed: Double?
    ) : Runnable
    {
        override fun run() {
            onStreetInfoChanged(requestInfo, streetName, maxSpeed)
        }
    }

    interface Listener {

        @MainThread
        fun onStreetInfoChanged(source: StreetNameCalculator,
                                newStreetName: String?,
                                newMaxSpeed: Double?)

    }

}
