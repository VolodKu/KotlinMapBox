package com.liveroads.mapzen

import com.liveroads.secrets.MAPZEN_API_KEY
import com.mapzen.valhalla.HttpHandler
import com.mapzen.valhalla.Route
import com.mapzen.valhalla.ValhallaRouter
import com.mapzen.valhalla.RouteCallback
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

/**
 * Created by Almond on 8/31/2017.
 */

class MapRouter {

    private var defaultCallback : RouteCallback? = null
    private var routerType : RouterType = RouterType.ROUTER_WALKING

    init {

    }

    fun setDefaultCallback(callback:RouteCallback) {
        defaultCallback = callback
    }

    fun setRouterType(type:RouterType) {
        routerType = type
    }

    fun callGetRouter(orgLat:Double, orgLon:Double, dstLat:Double, dstLon:Double)
    {
        var va_router = ValhallaRouter()

        va_router.setHttpHandler(SampleHttpHandler(HttpLoggingInterceptor.Level.BODY, MAPZEN_API_KEY))

        if (routerType == RouterType.ROUTER_WALKING)
        {
            va_router.setWalking()
        }
        else if (routerType == RouterType.ROUTER_DRIVING)
        {
            va_router.setDriving()
        }
        else if (routerType == RouterType.ROUTER_BIKING)
        {
            va_router.setBiking()
        }

        va_router.setLocation(doubleArrayOf(orgLat, orgLon))
        va_router.setLocation(doubleArrayOf(dstLat, dstLon))

        defaultCallback?.let { va_router.setCallback(it) }

        va_router.fetch()
    }

    companion object {
        fun callGetRouter(orgLat:Double, orgLon:Double, dstLat:Double, dstLon:Double, type: RouterType, callback:RouteCallback)
        {
            var va_router = ValhallaRouter()

            va_router.setHttpHandler(SampleHttpHandler(HttpLoggingInterceptor.Level.BODY, MAPZEN_API_KEY))

            if (type == RouterType.ROUTER_WALKING)
            {
                va_router.setWalking()
            }
            else if (type == RouterType.ROUTER_DRIVING)
            {
                va_router.setDriving()
            }
            else if (type == RouterType.ROUTER_BIKING)
            {
                va_router.setBiking()
            }

            va_router.setLocation(doubleArrayOf(orgLat, orgLon))
            va_router.setLocation(doubleArrayOf(dstLat, dstLon))

            callback.let { va_router.setCallback(it) }

            va_router.fetch()
        }
    }

    enum class RouterType{
        ROUTER_DRIVING,
        ROUTER_BIKING,
        ROUTER_WALKING
    }

    class SampleHttpHandler(logLevel: HttpLoggingInterceptor.Level, apiKey:String) : HttpHandler() {

        private val NAME_API_KEY = "api_key"
        private val API_KEY = apiKey

        init {
            configure(DEFAULT_URL, logLevel)
        }

        @Throws(IOException::class)
        protected override fun onRequest(chain: Interceptor.Chain): Response {
            val url = chain.request()
                    .url()
                    .newBuilder()
                    .addQueryParameter(NAME_API_KEY, API_KEY)
                    .build()

            return chain.proceed(chain.request().newBuilder().url(url).build())
        }
    }
}