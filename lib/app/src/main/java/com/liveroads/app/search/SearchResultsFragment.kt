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
import com.liveroads.mapzen.body.AutocompleteResponse
import com.liveroads.ui.search.SearchResultsView
import com.liveroads.ui.search.SearchResultsView.SearchResultsAdapter
import com.liveroads.util.log.*

class SearchResultsFragment : Fragment(), SearchWorkFragment.Listener, SearchResultsAdapter.Listener {

    private val logger = obtainLogger()

    private val searchResultsView: SearchResultsView
        get() = (view as SearchResultsView)

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
        return inflater.inflate(R.layout.lr_fragment_search_results, container, false)
    }

    override fun onDestroyView() {
        logger.onDestroyView()
        if (searchResultsView.adapter.listener === this) {
            searchResultsView.adapter.listener = null
        }
        super.onDestroyView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        updateUiVisibilities()
        handleSearchResult(workFragment.searchResult as? SearchTask.Result)
        lastWorkFragmentStateId = workFragment.stateId

        searchResultsView.errorRetryButton.setOnClickListener {
            val searchResult = workFragment.searchResult
            if (searchResult is SearchTask.Result.Failure) {
                workFragment.startSearch(searchResult.text)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
        searchResultsView.adapter.listener = this
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()

        workFragment.addListener(this)

        if (workFragment.stateId !== lastWorkFragmentStateId) {
            updateUiVisibilities()
            handleSearchResult(workFragment.searchResult as SearchTask.Result)
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
        handleSearchResult(workFragment.searchResult as? SearchTask.Result)
        lastWorkFragmentStateId = workFragment.stateId
    }

    private fun handleSearchResult(result: SearchTask.Result?) {
        positionMap = SparseIntArray()
        val errorDetails: String?
        val viewModels: List<SearchResultsView.ResultViewModel>

        when (result) {
            is SearchTask.Result.Failure -> {
                errorDetails = result.message
                viewModels = emptyList()
            }
            is SearchTask.Result.Success -> {
                errorDetails = null
                if (result.results == null) {
                    viewModels = emptyList()
                } else {
                    val mutableViewModels = mutableListOf<SearchResultsView.ResultViewModel>()
                    result.results.forEachIndexed { index, model ->
                        if (model != null) {
                            val label = model.properties?.name ?: ""
                            val region = model.properties?.region ?: ""
                            val distance = model.properties?.distance ?:0.0
                            val viewModel = SearchResultsView.ResultViewModel(label, region, distance)
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

        searchResultsView.apply {
            errorDetailsView.text = errorDetails
            adapter.items = viewModels
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateUiVisibilities() {
        val isSearchInProgress = workFragment.isSearchInProgress
        val searchResult = workFragment.searchResult as? SearchTask.Result

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
            is SearchTask.Result.Failure -> {
                spinnerVisible = false
                noResultsVisible = false
                resultsVisible = false
                errorVisible = true
                errorDetailsVisible = !searchResult.message.isNullOrBlank()
            }
            is SearchTask.Result.Success -> {
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

        searchResultsView.apply {
            spinnerView.visibility = if (spinnerVisible) View.VISIBLE else View.GONE
            noResultsView.visibility = if (noResultsVisible) View.VISIBLE else View.GONE
            recyclerView.visibility = if (resultsVisible) View.VISIBLE else View.GONE
            errorLayout.visibility = if (errorVisible) View.VISIBLE else View.GONE
            errorDetailsView.visibility = if (errorDetailsVisible) View.VISIBLE else View.GONE
        }
    }

    override fun onItemClicked(adapter: SearchResultsAdapter, position: Int) {
        val index = positionMap.get(position)
        val searchTaskResult = workFragment.searchResult as SearchTask.Result.Success
        val selectedResult = searchTaskResult.results!!.let { it[index] }
        listener?.onSearchResultSelected(this, selectedResult!!)
    }

    interface Listener {

        fun onSearchResultSelected(fragment: SearchResultsFragment, selectedResult: AutocompleteResponse.Feature)

    }

}
