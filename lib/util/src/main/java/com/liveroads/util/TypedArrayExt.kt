package com.liveroads.util

import android.content.res.TypedArray

inline fun TypedArray.use(block: TypedArray.() -> Unit) {
    try {
        block()
    } finally {
        recycle()
    }
}
