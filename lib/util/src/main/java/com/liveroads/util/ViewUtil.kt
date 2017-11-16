package com.liveroads.util

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

inline fun <reified T : View> View.findViewByIdOrThrow(id: Int): T {
    val view = findViewById<View>(id)
    if (view == null) {
        throw RuntimeException("view with ID $id not found in $this")
    } else if (view is T) {
        return view
    } else {
        throw RuntimeException("view with ID $id found in $this has wrong type: ${view::class} (expected ${T::class})")
    }
}

fun View.layout(bounds: Rect) {
    layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
}

fun View.getLayoutMargins(rect: Rect) {
    rect.left = 0
    rect.right = 0
    rect.top = 0
    rect.bottom = 0

    (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        rect.left = it.leftMargin
        rect.right = it.rightMargin
        rect.top = it.topMargin
        rect.bottom = it.bottomMargin
    }
}
