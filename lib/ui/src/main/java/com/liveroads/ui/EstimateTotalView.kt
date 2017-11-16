package com.liveroads.ui

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.widget.TextView

class EstimateTotalView : ConstraintLayout {

  private val timeView: TextView
  private val distanceView: TextView

  constructor(context: Context)
    : super(context)

  constructor(context: Context, attrs: AttributeSet)
    : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
    : super(context, attrs, defStyleAttr)

  init {
    inflate(context, R.layout.lr_view_estimate_total, this)
    timeView = findViewById(R.id.estimate_time)
    distanceView = findViewById(R.id.estimate_distance)
  }

  fun updateDistance(distance: String) {
    distanceView.text = distance
  }

  fun updateTime(time: String) {
    timeView.text = time
  }

}
