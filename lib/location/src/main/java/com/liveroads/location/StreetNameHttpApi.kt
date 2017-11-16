package com.liveroads.location

import com.liveroads.common.okhttp.liveroadsOkHttpClient
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

val streetNameHttpApi: StreetNameHttpApi = retrofit.create(StreetNameHttpApi::class.java)

data class GetStreetNameResponse(
        val highway: String?,
        val lanes: String?,
        val name: String?,
        val surface: String?,
        val maxspeed: Double?
)

interface StreetNameHttpApi {

    @GET("walid/lr/position.php")
    fun getStreetName(
            @Query("lat") latitude: String,
            @Query("lon") longitude: String
    ): Call<GetStreetNameResponse>

}
