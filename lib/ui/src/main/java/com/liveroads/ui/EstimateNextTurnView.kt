package com.liveroads.ui

import android.content.Context
import android.support.annotation.IntDef
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.widget.TextView

class EstimateNextTurnView : ConstraintLayout {
  private val streetView: TextView
  private val distanceView: TextView

  constructor(context: Context)
    : super(context)

  constructor(context: Context, attrs: AttributeSet)
    : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
    : super(context, attrs, defStyleAttr)

  @IntDef(LEFT, RIGHT)
  @Retention(AnnotationRetention.SOURCE)
  annotation class TurnDirection

    companion object {
      const val LEFT = 1L
      const val RIGHT = 2L
  }

  init {
    inflate(context, R.layout.lr_view_estimate_next_turn, this)
    streetView = findViewById(R.id.next_turn_street)
    distanceView = findViewById(R.id.next_turn_distance)
  }

  fun update(street: String, distance: String, @TurnDirection direction: Long) {
    streetView.text = street
    distanceView.text = distance
    applyDirection(direction)
  }

    private fun applyDirection(direction: Long) {
        when(direction) {
            LEFT -> streetView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.lr_ic_estimate_turn_left, 0, 0, 0)
            RIGHT -> streetView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.lr_ic_estimate_turn_right, 0, 0, 0)
        }
    }

}
