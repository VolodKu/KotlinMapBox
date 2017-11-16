package com.liveroads.ui

import android.animation.RectEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AccelerateDecelerateInterpolator
import com.liveroads.util.layout
import com.liveroads.util.ui.RequestLayoutOnChangeProperty
import com.liveroads.util.use

class PipView : ViewGroup {

    val largeView: View
        get() = getChildAt(0)
    val pipView: View
        get() = getChildAt(1)
    val largeEstimateView: EstimatePanelView
        get() = (getChildAt(0) as ViewGroup).getChildAt(1) as EstimatePanelView
    val pipEstimateView: EstimatePanelView
        get() = (getChildAt(1) as ViewGroup).getChildAt(1) as EstimatePanelView

    var pipMargin: Int by RequestLayoutOnChangeProperty(0)
    var pipWidthPercentage: Float by RequestLayoutOnChangeProperty(0.5f)
    var pipHeightPercentage: Float by RequestLayoutOnChangeProperty(0.5f)
    var pipEstimateHeight: Int by RequestLayoutOnChangeProperty(0)
    var pipVisible: Boolean by RequestLayoutOnChangeProperty(true)

    fun pipEstimateVisible(): Boolean = width > height

    private val layoutConfig = LayoutConfig()
    private val layoutBounds = LayoutBounds()

    val pipVisibilityAnimator = ValueAnimator().apply {
        setFloatValues(0f, 1f)
        duration = 300L
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            requestLayout()
        }
    }

    constructor(context: Context)
            : super(context) {
        applyAttributes(null)
    }

    constructor(context: Context, attrs: AttributeSet?)
            : super(context, attrs) {
        applyAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        applyAttributes(attrs, defStyleAttr)
    }

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        applyAttributes(attrs, defStyleAttr, defStyleRes)
    }

    fun animateTogglePipVisible() {
        animateSetPipVisible(!pipVisible)
    }

    fun animateSetPipVisible(visible: Boolean) {
        if (visible == pipVisible) {
            return
        }
        pipVisible = visible

        if (pipVisibilityAnimator.isStarted) {
            pipVisibilityAnimator.reverse()
        } else if (pipVisible) {
            pipVisibilityAnimator.start()
        } else {
            pipVisibilityAnimator.reverse()
        }
    }

    private fun applyAttributes(attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LiveRoadsUI, defStyleAttr, defStyleRes).use {
            if (hasValue(R.styleable.LiveRoadsUI_lr_pip_margin)) {
                pipMargin = getDimensionPixelSize(R.styleable.LiveRoadsUI_lr_pip_margin, pipMargin)
            }
            if (hasValue(R.styleable.LiveRoadsUI_lr_pip_width_pct)) {
                pipWidthPercentage = getFloat(R.styleable.LiveRoadsUI_lr_pip_width_pct, pipWidthPercentage)
            }
            if (hasValue(R.styleable.LiveRoadsUI_lr_pip_height_pct)) {
                pipHeightPercentage = getFloat(R.styleable.LiveRoadsUI_lr_pip_height_pct, pipHeightPercentage)
            }
            if (hasValue(R.styleable.LiveRoadsUI_lr_pip_estimate_height)) {
                pipEstimateHeight = getDimensionPixelSize(R.styleable.LiveRoadsUI_lr_pip_estimate_height, pipEstimateHeight)
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        (state as SavedState).let {
            super.onRestoreInstanceState(it.superState)
        }
    }

    override fun generateDefaultLayoutParams(): PipView.LayoutParams {
        return PipView.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    override fun generateLayoutParams(layoutParams: ViewGroup.LayoutParams?): PipView.LayoutParams {
        return when (layoutParams) {
            null -> generateDefaultLayoutParams()
            is PipView.LayoutParams -> PipView.LayoutParams(layoutParams)
            is ViewGroup.MarginLayoutParams -> PipView.LayoutParams(layoutParams)
            else -> PipView.LayoutParams(layoutParams)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): PipView.LayoutParams {
        return PipView.LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(layoutParams: ViewGroup.LayoutParams?): Boolean {
        return layoutParams is PipView.LayoutParams
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            throw IllegalArgumentException("width mode UNSPECIFIED not supported")
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            throw IllegalArgumentException("height mode UNSPECIFIED not supported")
        }

        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)

        val bounds = updateLayoutBounds(measuredWidth, measuredHeight)

        pipEstimateView.visibility = if (pipEstimateVisible()) View.VISIBLE else View.GONE
        largeEstimateView.visibility = if (!pipEstimateVisible()) View.VISIBLE else View.GONE

        measureChild(largeView, bounds.largeViewWidthMeasureSpec, bounds.largeViewHeightMeasureSpec)
        measureChild(pipView, bounds.pipViewWidthMeasureSpec, bounds.pipViewHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val bounds = updateLayoutBounds(right - left, bottom - top)
        largeView.layout(bounds.largeView)
        pipView.layout(bounds.pipView)
    }

    private fun updateLayoutBounds(width: Int, height: Int): LayoutBounds {
        layoutConfig.let {
            it.width = width
            it.height = height
            it.paddingLeft = paddingLeft
            it.paddingRight = paddingRight
            it.paddingTop = paddingTop
            it.paddingBottom = paddingBottom
            it.pipEstimateHeight = pipEstimateHeight
            it.pipWidthPercentage = pipWidthPercentage
            it.pipHeightPercentage = pipHeightPercentage
        }
        return layoutBounds.also {
            it.configure(layoutConfig)
            it.measure(pipVisible, pipEstimateVisible(), pipVisibilityAnimator)
        }
    }

    class SavedState : BaseSavedState, Parcelable {

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
        }

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel) = SavedState(parcel)
                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }

    }

    class LayoutParams : MarginLayoutParams {

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: PipView.LayoutParams) : super(source)

        constructor(source: ViewGroup.MarginLayoutParams) : super(source)

        constructor(source: ViewGroup.LayoutParams) : super(source)

    }

    private data class LayoutConfig(
            var width: Int,
            var height: Int,
            var paddingLeft: Int,
            var paddingRight: Int,
            var paddingTop: Int,
            var paddingBottom: Int,
            var pipEstimateHeight: Int,
            var pipWidthPercentage: Float,
            var pipHeightPercentage: Float) {

        constructor() : this(0, 0, 0, 0, 0, 0, 0, 0f, 0f)

        fun copyValuesFrom(other: LayoutConfig) {
            width = other.width
            height = other.height
            paddingLeft = other.paddingLeft
            paddingRight = other.paddingRight
            paddingTop = other.paddingTop
            paddingBottom = other.paddingBottom
            pipEstimateHeight = other.pipEstimateHeight
            pipWidthPercentage = other.pipWidthPercentage
            pipHeightPercentage = other.pipHeightPercentage
        }

    }

    private class LayoutBounds {

        val largeView = Rect()
        val pipView = Rect()

        val largeViewWidthMeasureSpec: Int
            get() = MeasureSpec.makeMeasureSpec(largeView.width(), MeasureSpec.EXACTLY)
        val largeViewHeightMeasureSpec: Int
            get() = MeasureSpec.makeMeasureSpec(largeView.height(), MeasureSpec.EXACTLY)
        val pipViewWidthMeasureSpec: Int
            get() = MeasureSpec.makeMeasureSpec(pipView.width(), MeasureSpec.EXACTLY)
        val pipViewHeightMeasureSpec: Int
            get() = MeasureSpec.makeMeasureSpec(pipView.height(), MeasureSpec.EXACTLY)

        private var configValid = false
        private var config = LayoutConfig()
        private var pipWidth = 0
        private var pipHeight = 0
        private val pipViewInvisibleBounds = Rect()
        private val pipViewVisibleBounds = Rect()
        private val pipViewVisibleWithEstimateBounds = Rect()
        private val rectEvaluator = RectEvaluator(Rect())

        fun configure(config: LayoutConfig) {
            if (configValid && config == this.config) {
                return // no need to re-measure
            }
            this.configValid = true
            this.config.copyValuesFrom(config)
            reconfigure()
        }

        fun reconfigure() {
            val availableWidth = Math.max(0, config.width - config.paddingLeft - config.paddingRight)
            val availableHeight = Math.max(0, config.height - config.paddingTop - config.paddingBottom)

            largeView.let {
                if (availableWidth == 0 || availableHeight == 0) {
                    it.setEmpty()
                } else {
                    it.left = config.paddingLeft
                    it.right = config.width - config.paddingRight
                    it.top = config.paddingTop
                    it.bottom = config.height - config.paddingBottom
                }
            }

            pipWidth = Math.max(0, Math.round(availableWidth * config.pipWidthPercentage))
            pipHeight = Math.max(0, Math.round(availableHeight * config.pipHeightPercentage))

            pipViewInvisibleBounds.let {
                it.left = availableWidth
                it.right = availableWidth
                it.top = 0
                it.bottom = 0
            }

            pipViewVisibleBounds.let {
                it.right = config.width - config.paddingRight
                it.left = it.right - pipWidth
                it.top = config.paddingTop
                it.bottom = it.top + pipHeight
            }

            pipViewVisibleWithEstimateBounds.let {
                it.right = config.width - config.paddingRight
                it.left = it.right - pipWidth
                it.top = config.paddingTop
                it.bottom = it.top + pipHeight + config.pipEstimateHeight
            }
        }

        fun measure(pipVisible: Boolean, pipEstimateVisible: Boolean, pipVisibilityAnimator: ValueAnimator) {
            val endBounds = when {
                (pipEstimateVisible && pipVisible) -> pipViewVisibleWithEstimateBounds
                pipVisible -> pipViewVisibleBounds
                else -> pipViewInvisibleBounds
            }
            pipView.set(when {
                pipVisibilityAnimator.isStarted -> {
                    rectEvaluator.evaluate(
                            pipVisibilityAnimator.animatedFraction,
                            pipViewInvisibleBounds,
                            endBounds)
                }
                else -> endBounds
            })
        }
    }

}
