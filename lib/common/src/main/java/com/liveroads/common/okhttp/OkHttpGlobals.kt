package com.liveroads.common.okhttp

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

val liveroadsOkHttpClient = OkHttpClient.Builder().run {
    connectTimeout(20, TimeUnit.SECONDS)
    readTimeout(20, TimeUnit.SECONDS)
    writeTimeout(20, TimeUnit.SECONDS)
    build()
}
