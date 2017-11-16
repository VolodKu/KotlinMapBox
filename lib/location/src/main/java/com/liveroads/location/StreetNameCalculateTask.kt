package com.liveroads.location

import android.os.AsyncTask
import android.support.annotation.MainThread
import com.liveroads.common.log.obtainLogger
import com.squareup.moshi.JsonDataException
import java.io.IOException
import java.text.DecimalFormat

class StreetNameCalculateTask(val requestInfo: RequestInfo, val callback: Callback)
    : AsyncTask<Unit, Unit, Unit>() {

    private val logger = obtainLogger()
    private lateinit var result: Result

    override fun doInBackground(vararg unused: Unit?) {
        result = run()
    }

    private fun run(): Result {
        val call = streetNameHttpApi.getStreetName(requestInfo.latitude, requestInfo.longitude)

        if (isCancelled) {
            return Result.CANCELLED
        }

        val url = call.request().url().toString()
        logger.d("HTTP request starting: %s", url)

        val response = try {
            call.execute()
        } catch (e: JsonDataException) {
            logger.w("HTTP request failed: %s (%s)", url, e)
            return Result.Failure(Result.Failure.ErrorCode.RESPONSE_JSON_INVALID, e.toString())
        } catch (e: IOException) {
            if (isCancelled) {
                return Result.CANCELLED
            } else {
                logger.w("HTTP request failed: %s (%s)", url, e)
                return Result.Failure(Result.Failure.ErrorCode.IO_EXCEPTION, e.toString())
            }
        }

        val body = response.body()
        if (body == null || !response.isSuccessful) {
            logger.w("HTTP request failed: %s (HTTP response code %d)", url, response.code())
            return Result.Failure(Result.Failure.ErrorCode.HTTP_RESPONSE_CODE, "HTTP response code ${response.code()}")
        }

        val streetName = body.name?.trim()
        return Result.Success(streetName, body.maxspeed)
    }

    override fun onPostExecute(unused: Unit?) {
        callback.onStreetNameCalculateTaskComplete(this, result)
    }

    interface Callback {

        @MainThread
        fun onStreetNameCalculateTaskComplete(task: StreetNameCalculateTask, result: Result)

    }

    sealed class Result {

        data class Success(val streetName: String?, val maxSpeed: Double?) : Result()
        data class Failure(val code: ErrorCode, val message: String?) : Result() {
            enum class ErrorCode {
                CANCELLED,
                IO_EXCEPTION,
                HTTP_RESPONSE_CODE,
                RESPONSE_JSON_INVALID,
            }
        }

        companion object {
            val CANCELLED = Failure(Failure.ErrorCode.CANCELLED, "operation cancelled")
        }

    }

    data class RequestInfo(val latitude: String, val longitude: String) {

        companion object {

            fun forLatitudeLongitude(latitude: Double, longitude: Double): RequestInfo? {
                return if (latitude.isNaN() || latitude.isInfinite() || longitude.isNaN() || longitude.isInfinite()) {
                    null
                } else {
                    RequestInfo(formatLatLon(latitude), formatLatLon(longitude))
                }
            }

        }

    }

}

private val threadLocalDecimalFormat = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue(): DecimalFormat {
        return DecimalFormat("#.#########")
    }
}

private fun formatLatLon(value: Double): String {
    val formatter = threadLocalDecimalFormat.get()
    return formatter.format(value)
}
