package com.liveroads.mapzen

import com.liveroads.common.okhttp.liveroadsOkHttpClient
import com.liveroads.mapzen.body.AutocompleteResponse
import com.liveroads.mapzen.body.SearchResponse
import com.squareup.moshi.Moshi
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private val moshi = Moshi.Builder().build()
private val retrofit = Retrofit.Builder().run {
    baseUrl("http://45.55.242.94")
    client(liveroadsOkHttpClient)
    addConverterFactory(MoshiConverterFactory.create(moshi))
    validateEagerly(true)
    build()
}

val poiHttpApi: PoiHttpApi = retrofit.create(PoiHttpApi::class.java)

data class POIResponse(
        val results: List<Result?>?
) {

    data class Result(
            val lat: String?,
            val lon: String?,
            val distance: Double?,
            val unit: String?,
            val address: Address?,
            val tags: Tags?
    ) {

        data class Address(
                val city: String?,
                val housenumber: String?,
                val postcode: String?,
                val zip: String?,
                val province: String?,
                val street: String?,
                val country: String?

                )

        data class Tags(
                val amenity: String?,
                val name: String?,
                val website: String?,
                val phone: String?
        )

    }

}

interface PoiHttpApi {

    @GET("walid/tiles/poi.php")
    fun getPois(
            @Query("location") location: String,
            @Query("type") type: String,
            @Query("values") values: String
    ): Call<POIResponse>

}

interface MapzenHttpApi {

    @GET("v1/autocomplete")
    fun autocomplete(
            @Query("api_key") apiKey: String,
            @Query("text") text: String,
            @Query("focus.point.lat") latitude: String?,
            @Query("focus.point.lon") longitude: String?
    ): Call<AutocompleteResponse>

    @GET("v1/search")
    fun searchName(
            @Query("api_key") apiKey: String,
            @Query("text") text: String,
            @Query("focus.point.lat") latitude: String?,
            @Query("focus.point.lon") longitude: String?,
            @Query("boundary.circle.lat") latitudeCircle: String?,
            @Query("boundary.circle.lon") longitudeCircle: String?,
            @Query("boundary.circle.radius") radius: Int?
    ): Call<SearchResponse>

}
