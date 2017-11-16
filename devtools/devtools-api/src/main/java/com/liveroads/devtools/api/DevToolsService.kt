package com.liveroads.devtools.api

import android.content.ComponentName
import android.os.IBinder

object DevToolsService {

    /**
     * Component name to use in an Intent to bind to the service.
     */
    val COMPONENT_NAME = ComponentName("com.liveroads.devtools", "com.liveroads.devtools.DevToolsService")

    /**
     * Broadcast Action sent when a DevTools setting may have changed.
     *
     * The recipient of this broadcast must possess the com.liveroads.permission.INTERNAL permission.
     */
    const val ACTION_DEV_TOOLS_SETTING_CHANGED = "liveroads.DevToolsService.ACTION_DEV_TOOLS_SETTING_CHANGED"

    /**
     * Verify that an [IBinder] object is a local instance of the IDevToolsService.
     * @param binder the object to verify; may be null, which will never be verified.
     * @return the verified service, or null if the given object was not verified.
     */
    fun verifyDevToolsBinder(binder: IBinder?): IDevToolsService? {
        // verify that the binder is indeed a local object (i.e. running in the same process), as opposed to a remote
        // binder; by doing so we guarantee that the other end is signed by the same certificate as us and therefore is
        // not being spoofed
        binder?.queryLocalInterface("com.liveroads.devtools.api.IDevToolsService") ?: return null
        return IDevToolsService.Stub.asInterface(binder)
    }

    const val KEY_MAP_TILT_ZOOM_VISIBLE = "MAP_TILT_ZOOM_VISIBLE"
    const val KEY_PIP_MAP_STYLE_URL = "KEY_PIP_MAP_STYLE_URL"
    const val KEY_LARGE_MAP_STYLE_URL = "KEY_LARGE_MAP_STYLE_URL"
    const val KEY_LOCATION_SOURCE = "KEY_LOCATION_SOURCE"
    const val KEY_USE_LARGE_MAP_CENTER_AS_LOCATION = "KEY_USE_LARGE_MAP_CENTER_AS_LOCATION"
    const val KEY_MAP_GRID_LINES_ENABLED= "KEY_MAP_GRID_LINES_ENABLED"

    enum class LocationSource {
        BLACKBOX,
        GMS,
    }

}
