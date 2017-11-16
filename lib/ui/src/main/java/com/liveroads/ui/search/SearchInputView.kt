package com.liveroads.ui.search

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.liveroads.ui.R
import com.liveroads.util.findViewByIdOrThrow
import android.support.v4.content.res.ResourcesCompat
import android.text.TextUtils

class SearchInputView : LinearLayout {

    var listener: Listener? = null

    val searchTextView: TextView

    val searchGasStation: ImageView
    val searchLocalPark: ImageView
    val searchRestaurant: ImageView

    val cancelSearch: ImageView
    val tabLayout : LinearLayout

    private val textWatcher : SearchTextWatcher

    constructor(context: Context)
            : super(context)

    constructor(context: Context, attrs: AttributeSet?)
            : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.lr_view_search_input, this)
        searchTextView = findViewByIdOrThrow(R.id.text_input_edittext)
        searchGasStation = findViewByIdOrThrow(R.id.search_gas_station)
        searchLocalPark = findViewByIdOrThrow(R.id.search_local_parking)
        searchRestaurant = findViewByIdOrThrow(R.id.search_restaurant)
        textWatcher = SearchTextWatcher()

        searchTextView.addTextChangedListener(textWatcher)
        searchTextView.setOnEditorActionListener(SearchEditorActionListener())

        cancelSearch = findViewByIdOrThrow(R.id.cancel_search)
        cancelSearch.setOnClickListener(SearchCancelListener())

        tabLayout = findViewByIdOrThrow(R.id.tab_establishment)

        SearchTabClickListener().let {
            searchGasStation.setOnClickListener(it)
            searchLocalPark.setOnClickListener(it)
            searchRestaurant.setOnClickListener(it)
        }
    }

    fun selectedTab(selected : Int) {
        val white : Int = ResourcesCompat.getColor(resources, android.R.color.white, context.theme)
        val black : Int = ResourcesCompat.getColor(resources, R.color.black75Alpha, context.theme)
        val grey : Int = ResourcesCompat.getColor(resources, android.R.color.darker_gray, context.theme)

        searchGasStation.setBackgroundColor(white)
        searchGasStation.setColorFilter(black, android.graphics.PorterDuff.Mode.SRC_IN)

        searchLocalPark.setBackgroundColor(white)
        searchLocalPark.setColorFilter(black, android.graphics.PorterDuff.Mode.SRC_IN)

        searchRestaurant.setBackgroundColor(white)
        searchRestaurant.setColorFilter(black, android.graphics.PorterDuff.Mode.SRC_IN)

        if (selected == 0)
        {
            searchGasStation.setBackgroundColor(grey)
            searchGasStation.setColorFilter(white, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        else if (selected == 1)
        {
            searchLocalPark.setBackgroundColor(grey)
            searchLocalPark.setColorFilter(white, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        else if (selected == 2)
        {
            searchRestaurant.setBackgroundColor(grey)
            searchRestaurant.setColorFilter(white, android.graphics.PorterDuff.Mode.SRC_IN)
        }
    }

    private inner class SearchTabClickListener : OnClickListener {

        override fun onClick(view: View) {
            if (view === searchGasStation) {
                selectedTab(0)
                searchTextView.removeTextChangedListener(textWatcher)
                searchTextView.text = "Gas Stations"
                listener?.onSearchEstablishment("amenity","fuel")
                searchTextView.addTextChangedListener(textWatcher)
            } else if (view === searchLocalPark) {
                selectedTab(1)
                searchTextView.removeTextChangedListener(textWatcher)
                searchTextView.text = "Parks"
                listener?.onSearchEstablishment("leisure","park")
                searchTextView.addTextChangedListener(textWatcher)
            } else if (view === searchRestaurant) {
                selectedTab(2)
                searchTextView.removeTextChangedListener(textWatcher)
                searchTextView.text = "Restaurant"
                listener?.onSearchEstablishment("amenity","restaurant,pub,bar")
                searchTextView.addTextChangedListener(textWatcher)
            } else {
                throw IllegalArgumentException("unknown view: $view")
            }
        }

    }

    private inner class SearchCancelListener : OnClickListener {

        override fun onClick(view: View) {
            listener?.onCancelSearch()
            searchTextView.text = ""
            tabLayout.visibility = View.VISIBLE
            selectedTab(-1)
        }

    }

    private inner class SearchTextWatcher : TextWatcher {

        override fun afterTextChanged(s: Editable) {
            listener?.onSearchTextChanged(this@SearchInputView, s)

            if (TextUtils.isEmpty(s.toString()))
            {
                tabLayout.visibility = View.VISIBLE
            }
            else
                tabLayout.visibility = View.GONE
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }

    }

    private inner class SearchEditorActionListener : TextView.OnEditorActionListener {

        override fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                listener?.onSearchClick(this@SearchInputView)
                return true
            } else {
                return false
            }
        }

    }

    interface Listener {

        fun onSearchTextChanged(view: SearchInputView, text: CharSequence)

        fun onSearchClick(view: SearchInputView)

        fun onSearchEstablishment(type: String,value: String)

        fun onCancelSearch()
    }

}
