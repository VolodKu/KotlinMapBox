package com.liveroads.util

import android.os.Parcel

fun Parcel.readBoolean() = (readInt() != 0)

fun Parcel.writeBoolean(value: Boolean) {
    writeInt(if (value) 1 else 0)
}
