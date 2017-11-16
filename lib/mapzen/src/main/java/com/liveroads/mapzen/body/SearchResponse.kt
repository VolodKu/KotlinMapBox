package com.liveroads.mapzen.body

/**
 * Created by Almond on 8/29/2017.
 */

data class SearchResponse(
        val features: List<Feature?>?
) {

    data class Feature(
            val type: String?,
            val geometry: Geometry?,
            val properties: Properties?
    ) {

        data class Geometry(
                val type: String?,
                val coordinates: List<Double>?
        )

        data class Properties(
                val id: String?,
                val name: String?,
                val distance: Double,
                val country: String?,
                val region: String?,
                val county: String?,
                val locality: String?,
                val label: String?
        )

    }

}