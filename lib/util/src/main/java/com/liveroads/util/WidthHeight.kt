package com.liveroads.util

data class WidthHeight(var width: Int = 0, var height: Int = 0) {

    fun setNonNegativeWidthRounded(value: Float) {
        width = Math.max(0, Math.round(value))
    }

    fun setNonNegativeHeightRounded(value: Float) {
        height = Math.max(0, Math.round(value))
    }

}
