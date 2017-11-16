package com.liveroads.app.search

import android.os.AsyncTask
import com.liveroads.common.log.obtainLogger
import com.liveroads.mapzen.POIResponse
import com.liveroads.mapzen.PoiHttpApi
import com.liveroads.mapzen.body.AutocompleteResponse
import com.liveroads.mapzen.body.SearchResponse
import com.liveroads.mapzen.mapzenHttpApi
import com.liveroads.mapzen.poiHttpApi
import com.liveroads.secrets.MAPZEN_API_KEY
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Locale

class SearchTask(
        val fragment: WeakReference<SearchWorkFragment>,
        val text: String,
        val latitude: Double,
        val longitude: Double,
        val searchType: SearchWorkFragment.SearchType,
        val type: String?,
        val values: String?
) : AsyncTask<Unit, Unit, Unit>() {

    private val logger = obtainLogger()

    private var result: Any? = null

    override fun doInBackground(vararg arg: Unit?) {
        if (searchType == SearchWorkFragment.SearchType.SEARCH_AUTOCOMPLETE) {
            result = run()
        }
        else
        {
            result = runPOI()
        }
    }

    private fun runPOI(): POIResult? {
        val latitudeStr = formatLatLong(latitude)
        val longitudeStr = formatLatLong(longitude)

        val call = poiHttpApi.getPois(latitudeStr+"," +longitudeStr,type!!, values!!)
//        val call = mapzenHttpApi.searchName(MAPZEN_API_KEY, text, latitudeStr, longitudeStr, latitudeStr, longitudeStr, 50)
        val url = call.request().url()

        if (isCancelled) {
            return null
        }

        logger.v("Performing search: %s", url)

        val response = try {
            call.execute()
        } catch (e: IOException) {
            return if (isCancelled) {
                null
            } else {
                logger.w(e, "searching for '%s' failed", text)
                POIResult.Failure(text, e.toString())
            }
        }

        if (isCancelled) {
            return null
        }

        val body = response.body()
        if (body == null || !response.isSuccessful) {
            logger.w("searching for '%s' failed: HTTP response code: %d", text, response.code())
            return POIResult.Failure(text, "HTTP response code: ${response.code()}")
        }

        logger.i("searching for '%s' complete: %d results", text, body.results?.size ?: 0)
        return POIResult.Success(text, body.results)

//        logger.i("searching for '%s' complete: %d results", text, body.features?.size ?: 0)
//        return POIResult.Success(text, body.features)
    }

    private fun run(): Result? {
        val latitudeStr = formatLatLong(latitude)
        val longitudeStr = formatLatLong(longitude)
        val call = mapzenHttpApi.autocomplete(MAPZEN_API_KEY, text, latitudeStr, longitudeStr)
        val url = call.request().url()

        if (isCancelled) {
            return null
        }

        logger.v("Performing search: %s", url)

        val response = try {
            call.execute()
        } catch (e: IOException) {
            return if (isCancelled) {
                null
            } else {
                logger.w(e, "searching for '%s' failed", text)
                Result.Failure(text, e.toString())
            }
        }

        if (isCancelled) {
            return null
        }

        val body = response.body()
        if (body == null || !response.isSuccessful) {
            logger.w("searching for '%s' failed: HTTP response code: %d", text, response.code())
            return Result.Failure(text, "HTTP response code: ${response.code()}")
        }

        logger.i("searching for '%s' complete: %d results", text, body.features?.size ?: 0)
        return Result.Success(text, body.features)
    }

    override fun onPostExecute(ignored: Unit?) {
        fragment.get()?.onSearchTaskComplete(this, result!!)
    }

    sealed class Result(val text: String) {
        class Success(text: String, val results: List<AutocompleteResponse.Feature?>?) : Result(text)
        class Failure(text: String, val message: String?) : Result(text)
    }
    sealed class POIResult(val text: String) {
        class Success(text: String, val results: List<POIResponse.Result?>?) : POIResult(text)
//        class Success(text: String, val results: List<SearchResponse.Feature?>?) : POIResult(text)
        class Failure(text: String, val message: String?) : POIResult(text)
    }


}

private fun formatLatLong(value: Double) = if (value.isNaN()) null else "%f".format(Locale.US, value)
