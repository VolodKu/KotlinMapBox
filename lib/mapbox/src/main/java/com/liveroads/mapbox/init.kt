package com.liveroads.mapbox

import android.app.Application
import com.liveroads.secrets.MAPBOX_API_KEY
import com.mapbox.mapboxsdk.Mapbox

/**
 * Initialize MapBox.
 *
 * <p>This method should be invoked exactly once from {@link Application#onCreate()}</p>
 *
 * @param app the application object
 */
fun init(app: Application) {
    Mapbox.getInstance(app, MAPBOX_API_KEY)
}
