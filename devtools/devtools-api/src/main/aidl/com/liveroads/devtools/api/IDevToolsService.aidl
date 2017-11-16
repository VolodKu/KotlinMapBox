package com.liveroads.devtools.api;

import android.os.Bundle;

///////////////////////////////////////////////////////////////////////////////
// NOTE: NEVER RE-ORDER THE METHODS DEFINED IN THIS INTERFACE AS DOING SO WILL
// BREAK BACKWARDS COMPATIBILITY.  ALL NEW METHODS MUST BE ADDED TO THE END
// IN ORDER TO MAINTAIN BACKWARDS COMPATIBILITY.
///////////////////////////////////////////////////////////////////////////////

interface IDevToolsService {

    /**
     * Get the version of this interface supported by the implementation.
     * This version number is incremented every time a new method is added.
     */
    int getVersion();

    /**
     * Get whether or not the implementation supports a certain feature.
     * @param featureName the name of the feature to query; may be null.
     * @return true if the given feature is supported, or false if the given feature is not supported, not known,
     * or is null.
     */
    boolean isFeatureSupported(String featureName);

    /**
     * Get whether or not DevTools options are enabled at all.
     * @return true if DevTools features are enabled, false if not.
     */
    boolean isEnabled();

    /**
     * Set whether or not DevTools features are enabled.
     * @return true if DevTools features are enabled, false if not.
     */
    void setEnabled(boolean enabled);

    /**
     * Call a method on the implementation.
     * @param method the name of the method to call; may be null, which does nothing and returns null.
     * @param arg an argument for the method; may be null.
     * @param args additional arguments for the method; may be null.
     * @return the result of the method; may be null.
     */
    Bundle call(String method, String arg, in Bundle args);

    /**
     * Clear all DevTools settings.
     */
    void clear();

    /**
     * Remove a DevTools setting.
     * @param key the key to remove; may be null, in which case this method does nothing.
     */
    void remove(String key);

    void setBoolean(String key, boolean value);
    boolean getBoolean(String key, boolean defaultValue);
    void setFloat(String key, float value);
    float getFloat(String key, float defaultValue);
    void setInt(String key, int value);
    int getInt(String key, int defaultValue);
    void setLong(String key, long value);
    long getLong(String key, long defaultValue);
    void setString(String key, String value);
    String getString(String key, String defaultValue);

}
