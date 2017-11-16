package com.liveroads.mapzen

import com.liveroads.common.okhttp.liveroadsOkHttpClient
import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private val moshi = Moshi.Builder().build()

private val retrofit = Retrofit.Builder().run {
    baseUrl("https://search.mapzen.com")
    client(liveroadsOkHttpClient)
    addConverterFactory(MoshiConverterFactory.create(moshi))
    validateEagerly(true)
    build()
}

val mapzenHttpApi = retrofit.create(MapzenHttpApi::class.java)
