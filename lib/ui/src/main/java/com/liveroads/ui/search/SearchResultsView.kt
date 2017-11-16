package com.liveroads.ui.search

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.liveroads.ui.R
import com.liveroads.util.findViewByIdOrThrow

class SearchResultsView : FrameLayout {

    val spinnerView: View
    val recyclerView: RecyclerView
    val noResultsView: View
    val errorLayout: ViewGroup
    val errorDetailsView: TextView
    val errorRetryButton: View
    val adapter: SearchResultsAdapter

    constructor(context: Context)
            : super(context)

    constructor(context: Context, attrs: AttributeSet?)
            : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.lr_view_search_results, this)
        spinnerView = findViewByIdOrThrow(R.id.lr_view_search_results_spinner)
        recyclerView = findViewByIdOrThrow(R.id.lr_view_search_results_recycler_view)
        noResultsView = findViewByIdOrThrow(R.id.lr_view_search_results_none)
        errorLayout = findViewByIdOrThrow(R.id.lr_view_search_results_error_layout)
        errorDetailsView = findViewByIdOrThrow(R.id.lr_view_search_results_error_details)
        errorRetryButton = findViewByIdOrThrow(R.id.lr_view_search_results_error_try_again)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = SearchResultsAdapter()
        recyclerView.adapter = adapter
    }

    data class ResultViewModel(
            val text: CharSequence,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
            val subText: CharSequence,
            val distance: Double
    )

    class SearchResultsAdapter : RecyclerView.Adapter<SearchResultViewHolder>(), SearchResultViewHolder.Listener {

        var items: List<ResultViewModel> = emptyList()
        var listener: Listener? = null

        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int) = position.toLong()

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SearchResultViewHolder(parent)

        override fun onBindViewHolder(vh: SearchResultViewHolder, position: Int) {
            vh.bind(items[position])
            vh.listener = this
        }

        override fun onViewRecycled(vh: SearchResultViewHolder) {
            vh.listener = null
            super.onViewRecycled(vh)
        }

        override fun onClick(vh: SearchResultViewHolder) {
            val position = vh.adapterPosition
            listener?.onItemClicked(this, position)
        }

        interface Listener {

            fun onItemClicked(adapter: SearchResultsAdapter, position: Int)

        }

    }

    class SearchResultViewHolder : RecyclerView.ViewHolder, View.OnClickListener {

        val view: SearchResultEntryView
        var listener: Listener? = null

        constructor(parent: ViewGroup) : this(inflateView(parent))

        private constructor(view: SearchResultEntryView) : super(view) {
            this.view = view
            view.setOnClickListener(this)
        }

        fun bind(model: ResultViewModel?) {
            view.titleView.text = model?.text
            view.subtitleView.text = model?.subText

            view.distance.text = java.lang.String.format("%.2fKm", model?.distance)
        }

        companion object {
            fun inflateView(parent: ViewGroup): SearchResultEntryView {
                return LayoutInflater.from(parent.context).inflate(R.layout.lr_vh_search_result_entry, parent, false)
                        as SearchResultEntryView
            }
        }

        override fun onClick(view: View?) {
            listener?.onClick(this)
        }

        interface Listener {

            fun onClick(vh: SearchResultViewHolder)

        }

    }

}

