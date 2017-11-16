package com.liveroads.login

import android.os.Parcel
import android.os.Parcelable

class LoginTaskResult : Parcelable {

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<LoginTaskResult> = CreatorImpl()
    }

    val error: Error?
    val errorMessage: String?

    constructor(error: Error?, errorMessage: String?) {
        this.error = error
        this.errorMessage = errorMessage
    }

    constructor(parcel: Parcel) {
        if (parcel.readInt() == 0) {
            error = null
        } else {
            val errorOrdinal = parcel.readInt()
            error = Error.values()[errorOrdinal]
        }
        errorMessage = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        if (error == null) {
            parcel.writeInt(0)
        } else {
            parcel.writeInt(1)
            parcel.writeInt(error.ordinal)
        }
        parcel.writeString(errorMessage)
    }

    override fun describeContents() = 0

    enum class Error {
        WEAK_PASSWORD,
        MALFORMED_EMAIL,
        INVALID_USER,
        INVALID_PASSWORD,
        ALREADY_EXISTS,
        NETWORK_ERROR,
        UNKNOWN
    }

    private class CreatorImpl : Parcelable.Creator<LoginTaskResult> {

        override fun createFromParcel(parcel: Parcel) = LoginTaskResult(parcel)

        override fun newArray(size: Int) = arrayOfNulls<LoginTaskResult>(size)
    }

}
