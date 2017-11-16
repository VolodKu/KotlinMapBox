package com.liveroads.app.search

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liveroads.app.R
import com.liveroads.common.log.obtainLogger
import com.liveroads.ui.search.SearchInputView
import com.liveroads.util.log.*

class SearchInputFragment : Fragment(), SearchInputView.Listener {

    private val logger = obtainLogger()
    private val searchInputView: SearchInputView
        get() = view as SearchInputView

    lateinit var workFragment: SearchWorkFragment
    lateinit var parentFragment: SearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_search_input, container, false)
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
        searchInputView.listener = this
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()
    }

    override fun onStop() {
        logger.onStop()
        super.onStop()
    }

    override fun onSearchEstablishment(type:String, values: String) {

        parentFragment.showPOIResultFragment(true)
        workFragment.setSearchType(SearchWorkFragment.SearchType.SEARCH_ESTABLISHMENT)

//        if (text == "Restaurants")
//        {
//            workFragment.setSearchType(SearchWorkFragment.SearchType.SEARCH_YELP)
//        }

        workFragment.clearSearchResult()
        workFragment.stopSearch()
//        val validatedSearchText = validateSearchText(text)
//        if (validatedSearchText != null) {
            workFragment.startPOISearch(type, values)
//        }
    }


    override fun onSearchTextChanged(view: SearchInputView, text: CharSequence) {
        if (TextUtils.isEmpty(text))
        {
            parentFragment.showResultFragment(false)
            return
        }

        parentFragment.showResultFragment(true)
        workFragment.setSearchType(SearchWorkFragment.SearchType.SEARCH_AUTOCOMPLETE)

        workFragment.clearSearchResult()
        workFragment.stopSearch()
        val validatedSearchText = validateSearchText(text)
        if (validatedSearchText != null) {
            workFragment.startSearchDelayed(validatedSearchText)
        }
    }

    override fun onSearchClick(view: SearchInputView) {
        val validatedSearchText = validateSearchText(searchInputView.searchTextView.text)
        if (validatedSearchText != null) {
            workFragment.startSearch(validatedSearchText)
        }
    }

    override fun onCancelSearch() {
        workFragment.clearSearchResult()
        workFragment.stopSearch()

        parentFragment.showResultFragment(false)
    }

    private fun validateSearchText(s: CharSequence?): String? {
        val cleaned = s?.toString()?.trim()
        return if (cleaned.isNullOrEmpty()) null else cleaned
    }

}
