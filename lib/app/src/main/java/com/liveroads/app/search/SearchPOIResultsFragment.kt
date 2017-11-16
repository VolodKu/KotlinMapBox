package com.liveroads.app.search

import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liveroads.app.R
import com.liveroads.common.log.obtainLogger
import com.liveroads.mapzen.POIResponse
import com.liveroads.ui.search.SearchPOIView
import com.liveroads.util.log.*

class SearchPOIResultsFragment : Fragment(), SearchWorkFragment.Listener, SearchPOIView.SearchPOIResultsAdapter.Listener {

    private val logger = obtainLogger()

    private val searchPOIView: SearchPOIView
        get() = (view as SearchPOIView)

    lateinit var workFragment: SearchWorkFragment
    var listener: Listener? = null

    private var lastWorkFragmentStateId: IBinder? = null
    private lateinit var positionMap: SparseIntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_search_poi_results, container, false)
    }

    override fun onDestroyView() {
        logger.onDestroyView()
        if (searchPOIView.adapter.listener === this) {
            searchPOIView.adapter.listener = null
        }
        super.onDestroyView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        updateUiVisibilities()
        handleSearchResult(workFragment.searchResult as? SearchTask.POIResult)
        lastWorkFragmentStateId = workFragment.stateId

        searchPOIView.errorRetryButton.setOnClickListener {
            val searchResult = workFragment.searchResult
            if (searchResult is SearchTask.Result.Failure) {
                workFragment.startSearch(searchResult.text)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
        searchPOIView.adapter.listener = this
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()

        workFragment.addListener(this)

        if (workFragment.stateId !== lastWorkFragmentStateId) {
            updateUiVisibilities()
            handleSearchResult(workFragment.searchResult as SearchTask.POIResult)
            lastWorkFragmentStateId = workFragment.stateId
        }
    }

    override fun onStop() {
        logger.onStop()
        workFragment.removeListener(this)
        super.onStop()
    }

    override fun onStateChanged(fragment: SearchWorkFragment) {
        updateUiVisibilities()
        handleSearchResult(workFragment.searchResult as? SearchTask.POIResult)
        lastWorkFragmentStateId = workFragment.stateId
    }

    private fun handleSearchResult(result: SearchTask.POIResult?) {
        positionMap = SparseIntArray()
        val errorDetails: String?
        val viewModels: List<SearchPOIView.ResultViewModel>

        when (result) {
            is SearchTask.POIResult.Failure -> {
                errorDetails = result.message
                viewModels = emptyList()
            }
            is SearchTask.POIResult.Success -> {
                errorDetails = null
                if (result.results == null) {
                    viewModels = emptyList()
                } else {
                    val mutableViewModels = mutableListOf<SearchPOIView.ResultViewModel>()
                    result.results.forEachIndexed { index, model ->
                        if (model != null) {
                            val label = model.tags?.name ?: ""
                            val region = model.address?.city ?: ""
                            val distance = model.distance ?:0.0
                            val unit = model.unit ?:"m"
                            val viewModel = SearchPOIView.ResultViewModel(label, region, unit ,distance)
                            positionMap.put(mutableViewModels.size, index)
                            mutableViewModels.add(viewModel)
                        }
                    }
                    viewModels = mutableViewModels
                }
            }
            null -> {
                errorDetails = null
                viewModels = emptyList()
            }
        }

        searchPOIView.apply {
            errorDetailsView.text = errorDetails
            adapter.items = viewModels
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateUiVisibilities() {
        val isSearchInProgress = workFragment.isSearchInProgress
        val searchResult = workFragment.searchResult as? SearchTask.POIResult

        val spinnerVisible: Boolean
        val noResultsVisible: Boolean
        val resultsVisible: Boolean
        val errorVisible: Boolean
        val errorDetailsVisible: Boolean

        if (isSearchInProgress) {
            spinnerVisible = true
            noResultsVisible = false
            resultsVisible = false
            errorVisible = false
            errorDetailsVisible = false
        } else when (searchResult) {
            is SearchTask.POIResult.Failure -> {
                spinnerVisible = false
                noResultsVisible = false
                resultsVisible = false
                errorVisible = true
                errorDetailsVisible = !searchResult.message.isNullOrBlank()
            }
            is SearchTask.POIResult.Success -> {
                val hasOneOrMoreResults = (searchResult.results != null && searchResult.results.isNotEmpty())
                spinnerVisible = false
                noResultsVisible = !hasOneOrMoreResults
                resultsVisible = hasOneOrMoreResults
                errorVisible = false
                errorDetailsVisible = false
            }
            null -> {
                spinnerVisible = false
                noResultsVisible = false
                resultsVisible = false
                errorVisible = false
                errorDetailsVisible = false
            }
        }

        searchPOIView.apply {
            spinnerView.visibility = if (spinnerVisible) View.VISIBLE else View.GONE
            noResultsView.visibility = if (noResultsVisible) View.VISIBLE else View.GONE
            recyclerView.visibility = if (resultsVisible) View.VISIBLE else View.GONE
            errorLayout.visibility = if (errorVisible) View.VISIBLE else View.GONE
            errorDetailsView.visibility = if (errorDetailsVisible) View.VISIBLE else View.GONE
        }
    }

    override fun onItemClicked(adapter: SearchPOIView.SearchPOIResultsAdapter, position: Int) {
        val index = positionMap.get(position)
        val searchTaskResult = workFragment.searchResult as SearchTask.POIResult.Success
        val selectedResult = searchTaskResult.results!!.let { it[index] }
        listener?.onSearchResultSelected(this, selectedResult!!)
    }

    interface Listener {

        fun onSearchResultSelected(fragment: SearchPOIResultsFragment, selectedResult: POIResponse.Result)

    }

}
