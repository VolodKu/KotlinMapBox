package com.liveroads.ui

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.widget.FrameLayout

class EstimatePanelView : FrameLayout {

  private val totalView: EstimateTotalView
  private val nextTurnView: EstimateNextTurnView

  constructor(context: Context)
    : super(context)

  constructor(context: Context, attrs: AttributeSet?)
    : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int)
    : super(context, attrs, defStyleAttr)

  init {
    inflate(context, R.layout.lr_view_estimate_panel, this)

    totalView = findViewById(R.id.estimate_total)
    nextTurnView = findViewById(R.id.estimate_next_turn)
  }

  private fun showTotal() {
    totalView.visibility = VISIBLE
    nextTurnView.visibility = GONE
  }

  private fun showNextTurn() {
    totalView.visibility = GONE
    nextTurnView.visibility = VISIBLE
  }

  fun updateNextTurn(dist: Long, streetName: String,
                     @EstimateNextTurnView.TurnDirection direction: Long)
  {
      val d = EstimationUtils.convertDistance(dist)
      nextTurnView.update(streetName, d, direction)

      if (dist > 500) {
          showTotal()
      } else {
          showNextTurn()
      }
  }

    fun updateTotal(dist: Long, time: Long) {
        val d = EstimationUtils.convertDistance(dist)
        val t = EstimationUtils.convertTime(time)
        totalView.updateDistance(d)
        totalView.updateTime(t)
    }



}
