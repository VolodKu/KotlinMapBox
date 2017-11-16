package com.liveroads.util.ui

import android.view.View
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Property delegate that invokes [View.requestLayout] on the owning view when the value of the property changes.
 */
class RequestLayoutOnChangeProperty<in R : View, T>(initialValue: T) : ReadWriteProperty<R, T> {

    private var value: T = initialValue

    override fun getValue(thisRef: R, property: KProperty<*>) = value

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        if (value != this.value) {
            this.value = value
            thisRef.requestLayout()
        }
    }

}
