package com.liveroads.ui.search

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.liveroads.ui.R
import com.liveroads.util.findViewByIdOrThrow

class SearchResultEntryView : LinearLayout {

    val titleView: TextView
    val subtitleView: TextView
    val distance : TextView

    constructor(context: Context)
            : super(context)

    constructor(context: Context, attrs: AttributeSet?)
            : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        inflate(context, R.layout.lr_view_search_result_entry, this)
        titleView = findViewByIdOrThrow(R.id.title)
        subtitleView = findViewByIdOrThrow(R.id.subtitle)
        distance = findViewByIdOrThrow(R.id.distance)
    }

}
