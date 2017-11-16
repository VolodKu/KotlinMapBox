package com.liveroads.app.search

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liveroads.app.R
import com.liveroads.common.log.obtainLogger
import com.liveroads.db.NavigationItem
import com.liveroads.mapzen.POIResponse
import com.liveroads.mapzen.body.AutocompleteResponse
import com.liveroads.mapzen.body.SearchResponse
import com.liveroads.util.log.*

class SearchFragment : Fragment(), SearchResultsFragment.Listener, SearchPOIResultsFragment.Listener, SearchHomeFragment.Listener {

    private val logger = obtainLogger()

    lateinit var workFragment: SearchWorkFragment
    var listener: Listener? = null

    private val searchInputFragment: SearchInputFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_search_input)
                as SearchInputFragment
    private val searchResultsFragment: SearchResultsFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_search_results)
                as SearchResultsFragment
    private val searchHomeFragment: SearchHomeFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_search_home)
                as SearchHomeFragment
    private val searchPOIResultsFragment: SearchPOIResultsFragment
        get() = childFragmentManager.findFragmentById(R.id.lr_fragment_search_poi_results)
                as SearchPOIResultsFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_search, container, false)
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
        searchInputFragment.workFragment = workFragment
        searchResultsFragment.workFragment = workFragment
        searchInputFragment.parentFragment = this
        searchPOIResultsFragment.workFragment = workFragment

        var transaction = this.activity.supportFragmentManager.beginTransaction()
        transaction?.hide(searchResultsFragment)
        transaction?.commit()

    }

    override fun onStart() {
        logger.onStart()
        super.onStart()
        searchResultsFragment.listener = this
        searchPOIResultsFragment.listener = this
        searchHomeFragment.listener = this
    }

    override fun onStop() {
        logger.onStop()
        if (searchResultsFragment.listener === this) {
            searchResultsFragment.listener = null
            searchPOIResultsFragment.listener = null
            searchHomeFragment.listener = null
        }
        super.onStop()
    }

    override fun onSearchResultSelected(fragment: SearchHomeFragment, selectedResult: NavigationItem) {
        val label = selectedResult.location
        val latitude = selectedResult.latitude
        val longitude = selectedResult.longitude
        val searchResult = SearchResult(label, selectedResult.province, latitude, longitude, selectedResult.distance)
        listener?.onSearchResultSelected(this, searchResult)
    }

    override fun onSearchResultSelected(fragment: SearchResultsFragment, selectedResult: AutocompleteResponse.Feature) {
        val label = selectedResult.properties?.label
        val coordinates = selectedResult.geometry?.coordinates
        val latitude = if (coordinates == null || coordinates.isEmpty()) Double.NaN else coordinates[1]
        val longitude = if (coordinates == null || coordinates.isEmpty()) Double.NaN else coordinates[0]
        val searchResult = SearchResult(label, selectedResult.properties?.region, latitude, longitude, selectedResult.properties?.distance)
        listener?.onSearchResultSelected(this, searchResult)
    }

    override fun onSearchResultSelected(fragment: SearchPOIResultsFragment, selectedResult: POIResponse.Result) {
        val label = selectedResult.tags?.name
        val latitude = selectedResult.lat!!.toDouble()
        val longitude = selectedResult.lon!!.toDouble()
        val searchResult = SearchResult(label, selectedResult.address?.city, latitude, longitude, selectedResult.distance)
        listener?.onSearchPOIResultSelected(this, searchResult)
    }

    fun showResultFragment(visible: Boolean)
    {
        var transaction = this.activity.supportFragmentManager.beginTransaction()

        if (visible)
        {
            transaction?.show(searchResultsFragment)
            transaction?.hide(searchHomeFragment)
            transaction?.hide(searchPOIResultsFragment)
        }
        else
        {
            transaction?.hide(searchResultsFragment)
            transaction?.hide(searchPOIResultsFragment)
            transaction?.show(searchHomeFragment)
        }

        transaction?.commit()
    }

    fun showPOIResultFragment(visible: Boolean)
    {
        var transaction = this.activity.supportFragmentManager.beginTransaction()

        if (visible)
        {
            transaction?.show(searchPOIResultsFragment)
            transaction?.hide(searchHomeFragment)
            transaction?.hide(searchResultsFragment)
        }
        else
        {
            transaction?.hide(searchResultsFragment)
            transaction?.hide(searchPOIResultsFragment)
            transaction?.show(searchHomeFragment)
        }

        transaction?.commit()
    }

    interface Listener {

        fun onSearchResultSelected(fragment: SearchFragment, searchResult: SearchResult)

        fun onSearchPOIResultSelected(fragment: SearchFragment, searchResult: SearchResult)

    }

    data class SearchResult(
            val label: String?,
            val province: String?,
            val latitude: Double,
            val longitude: Double,
            val distance : Double?
    )

}
