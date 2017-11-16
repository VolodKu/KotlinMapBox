package com.liveroads.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.annotation.MainThread
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.liveroads.blackbox.Data
import com.liveroads.common.devtools.devTools
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.api.DevToolsService.LocationSource
import com.liveroads.devtools.client.monitor.LocationSourceMonitor
import com.liveroads.util.log.*
import java.util.*
import java.util.concurrent.TimeUnit
import com.liveroads.blackbox.sdk as LiveRoadsBlackBox

private const val REQUEST_LOCATION_PERMISSION = 1
private const val REQUEST_RESOLVE_LOCATION_SETTINGS = 2

class LocationProviderFragment : Fragment(), LiveRoadsBlackBox.DataListener {

    private companion object {
        val DEFAULT_LOCATION_SOURCE = LocationSource.BLACKBOX
        val STREET_NAME_UPDATE_INTERVAL_MS = 2000L
    }

    var forcedLocationProvider: ForcedLocationProvider? = null
        set(provider) {
            if (field !== provider) {
                field = provider
                onLocationChanged(true)
            }
        }

    val location: Location?
        get() = forcedLocationProvider.let { if (it != null) it.location else realLocation }

    val streetName: String?
        get() = streetNameCalculator.streetName

    val maxSpeed: Double?
        get() = streetNameCalculator.maxSpeed

    private var realLocation: Location? = null

    private val logger = obtainLogger()
    private val listeners = mutableListOf<Listener>()
    private val blackBox = LiveRoadsBlackBox()
    private val locationSourceMonitor = object : LocationSourceMonitor(devTools) {
        override fun onValueChanged() {
            locationSource = value ?: DEFAULT_LOCATION_SOURCE
        }
    }

    var locationPermissionGranted = false
        private set
    private var lastKnownLocationTask: Task<Location>? = null
    private var locationSource = DEFAULT_LOCATION_SOURCE

    private val locationRequest = LocationRequest().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        expirationTime = Long.MAX_VALUE
        interval = 10
        fastestInterval = 10
    }

    private lateinit var locationClient: FusedLocationProviderClient

    private val streetNameCalculator = StreetNameCalculator(STREET_NAME_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS)

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        retainInstance = true

        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION).let { permission ->
            locationPermissionGranted = (permission == PackageManager.PERMISSION_GRANTED)
        }

        streetNameCalculator.onCreate()
        streetNameCalculator.listener = streetNameCalculatorListener

        locationClient = LocationServices.getFusedLocationProviderClient(context)

        blackBox.init(context.assets)
        blackBox.setDataListener(this)

        if (locationPermissionGranted) {
            lastKnownLocationTask.let { task ->
                if (task == null || task.isComplete) {
                    lastKnownLocationTask = locationClient.lastLocation.apply {
                        addOnSuccessListener(lastKnownLocationSuccessListener)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        logger.onDestroy()
        blackBox.setDataListener(null)
        streetNameCalculator.listener = null
        streetNameCalculator.onDestroy()
        super.onDestroy()
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()

        locationSourceMonitor.start()

        if (!locationPermissionGranted) {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            requestPermissions(permissions, REQUEST_LOCATION_PERMISSION)
        } else {
            startLocationTracking()
            blackBox.resume()
            verifyLocationSettings()
        }
    }

    override fun onStop() {
        logger.onStop()
        if (locationPermissionGranted) {
            blackBox.pause()
            stopLocationTracking()
        }

        locationSourceMonitor.stop()

        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        logger.logLifecycle("onRequestPermissionsResult() requestCode=%d permissions=%s grantResults=%s", requestCode,
                Arrays.toString(permissions), Arrays.toString(grantResults))
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            var allSuccess = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allSuccess = false
                }
            }
            if (allSuccess) {
                locationPermissionGranted = true
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun verifyLocationSettings() {
        val locationSettingsRequest = LocationSettingsRequest.Builder().run {
            addLocationRequest(locationRequest)
            build()
        }

        val settingsClient = LocationServices.getSettingsClient(context)
        val task = settingsClient.checkLocationSettings(locationSettingsRequest)
        task.addOnCompleteListener { it ->
            if (it.isSuccessful) {
                logger.d("Location settings verified")
            } else {
                val statusCode = (it.exception as ApiException).statusCode
                val statusMessage = (it.exception as ApiException).statusMessage
                logger.i("Location settings verification failed: %s", statusMessage)
                if (statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    logger.i("Starting location settings resolution activity")
                    val resolvable = it.exception as ResolvableApiException
                    resolvable.startResolutionForResult(activity, REQUEST_RESOLVE_LOCATION_SETTINGS)
                } else {
                    logger.w(it.exception, "unable to resolve location settings verification failure")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        logger.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RESOLVE_LOCATION_SETTINGS) {
            // no-op
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @MainThread
    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    @MainThread
    fun removeListener(listener: Listener) {
        for (i in listeners.size - 1 downTo 0) {
            if (listeners[i] === listener) {
                listeners.removeAt(i)
                break
            }
        }
    }

    @MainThread
    private fun updateLastKnownLocation(location: Location) {
        if (this.location == null && location.isValidForMapbox()) {
            this.realLocation = location
            blackBox.updateGPS(location)
            if (forcedLocationProvider == null) {
                listeners.forEach { it.onLocationChanged(this, location) }
            }
        }
    }

    @MainThread
    private fun updateLocation(location: Location, locationSource: LocationSource) {
        if (locationSource == this.locationSource && location.isValidForMapbox()) {
            this.realLocation = location
            if (forcedLocationProvider == null) {
                onLocationChanged(false)
            }
        }
    }

    @MainThread
    private fun updateSpeed(speed: Float?) {
        speed?.let { s ->
            listeners.forEach { it.onSpeedChanged(this, s.toDouble()) }
        }
    }

    @MainThread
    fun onLocationChanged(forceStreetNameCalculation: Boolean) {
        val location = this.location ?: return
        streetNameCalculator.onLocationChanged(location, forceStreetNameCalculation)
        listeners.forEach { it.onLocationChanged(this, location) }
    }

    private val lastKnownLocationSuccessListener = OnSuccessListener<Location> { location ->
        updateLastKnownLocation(location)
    }

    private fun startLocationTracking() {
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationTracking() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDataChanged(data: Data?) {
        val location = data?.asLocation()
        if (location != null) {
            updateLocation(location, LocationSource.BLACKBOX)
        }
        updateSpeed(data?.speed)
    }

    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult?) {
            val location = result?.lastLocation
            if (location != null) {
                blackBox.updateGPS(location)
                updateLocation(location, LocationSource.GMS)
            }
        }

    }

    private val streetNameCalculatorListener = object : StreetNameCalculator.Listener {

        override fun onStreetInfoChanged(source: StreetNameCalculator,
                                         newStreetName: String?,
                                         newMaxSpeed: Double?)
        {
            listeners.forEach {
                it.onStreetInfoChanged(this@LocationProviderFragment, newStreetName, newMaxSpeed)
            }
        }

    }

    interface Listener {

        @MainThread
        fun onLocationChanged(fragment: LocationProviderFragment, location: Location)

        @MainThread
        fun onStreetInfoChanged(fragment: LocationProviderFragment, newStreetName: String?, newMaxSpeed: Double?)

        @MainThread
        fun onSpeedChanged(fragment: LocationProviderFragment, speed: Double)

    }

    interface ForcedLocationProvider {

        val location: Location?

    }

}

private fun Data.asLocation() = Location("LiveRoadsBlackBox").also {
    it.accuracy = accuracy
    it.bearing = bearing
    it.elapsedRealtimeNanos = ts
    it.latitude = latitude.toDouble()
    it.longitude = longitude.toDouble()
    it.speed = speed
    it.time = System.currentTimeMillis()
}

private fun Location.isValidForMapbox(): Boolean {
    if (latitude.isNaN() || latitude.isInfinite()) {
        return false
    }
    if (longitude.isNaN() || longitude.isInfinite()) {
        return false
    }
    if (latitude == 0.0 && longitude == 0.0) {
        return false
    }
    return true
}
