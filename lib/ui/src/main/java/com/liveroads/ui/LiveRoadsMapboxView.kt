package com.liveroads.ui

import android.content.Context
import android.graphics.PointF
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.liveroads.util.findViewByIdOrThrow

class LiveRoadsMapboxView : ConstraintLayout {

    val mapContainer: FrameLayout
    val txtStreetName: TextView
    val speedLimitSign: SpeedLimitSignView
    val placeholderCenterView: View
    val placeholderAboveStreetNameView: View
    val debugInfoViewGroup: ViewGroup
    val txtDebugLatitude: TextView
    val txtDebugLongitude: TextView
    val txtDebugAltitude: TextView
    val txtDebugBearing: TextView
    val txtDebugTilt: TextView
    val txtDebugZoom: TextView
    val estimationPanelHolder: View

    var isTouchEventsEnabled = true

    var streetNameXBias = Float.NaN
        set(value) {
            field = value
            requestLayout()
        }

    private val pointF = PointF()

    constructor(context: Context)
            : super(context)

    constructor(context: Context, attrs: AttributeSet?)
            : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.lr_view_live_roads_mapbox, this)
        mapContainer = findViewByIdOrThrow(R.id.map_container)
        txtStreetName = findViewByIdOrThrow(R.id.street_name)
        speedLimitSign = findViewByIdOrThrow(R.id.speed_limit_sign)
        placeholderCenterView = findViewByIdOrThrow(R.id.placeholder_center)
        placeholderAboveStreetNameView = findViewByIdOrThrow(R.id.placeholder_above_street_name)
        debugInfoViewGroup = findViewByIdOrThrow(R.id.debug_info_layout)
        txtDebugLatitude = findViewByIdOrThrow(R.id.debug_latitude)
        txtDebugLongitude = findViewByIdOrThrow(R.id.debug_longitude)
        txtDebugAltitude = findViewByIdOrThrow(R.id.debug_altitude)
        txtDebugBearing = findViewByIdOrThrow(R.id.debug_bearing)
        txtDebugTilt = findViewByIdOrThrow(R.id.debug_tilt)
        txtDebugZoom = findViewByIdOrThrow(R.id.debug_zoom)
        estimationPanelHolder = findViewByIdOrThrow(R.id.estimation_panel_place_holder)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return if (!isTouchEventsEnabled) {
            true
        } else {
            super.onInterceptTouchEvent(event)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (!isTouchEventsEnabled) {
            true
        } else {
            return super.onTouchEvent(event)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        onMeasureEstimationPlaceHolder(widthMeasureSpec, heightMeasureSpec)
        onMeasureStreetName(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun onMeasureEstimationPlaceHolder(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChild(estimationPanelHolder, widthMeasureSpec, heightMeasureSpec)

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            throw IllegalArgumentException("a width must be specified")
        }

        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)

        val estimatePanelVisibleOnLargeMap = availableWidth > 0 && availableWidth > availableHeight
        val lp = estimationPanelHolder.layoutParams
        if (estimatePanelVisibleOnLargeMap) {
            lp.height = 1
        } else {
            lp.height = resources.getDimensionPixelSize(R.dimen.lr_estimate_panel_height)
        }
        estimationPanelHolder.layoutParams = lp
    }

    /**
     * Set the horizontal bias on the street name view so that it is centered horizontally about the
     * center of the screen (if streetNameXBias==0.5) or biased towards one side of the screen if
     * it is some other value.
     */
    private fun onMeasureStreetName(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChild(txtStreetName, widthMeasureSpec, heightMeasureSpec)

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            throw IllegalArgumentException("a width must be specified")
        }

        val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val bias = calculateXBiasFromMeasuredWidth(txtStreetName.measuredWidth, availableWidth,
                streetNameXBias)
        val lp = txtStreetName.layoutParams as ConstraintLayout.LayoutParams
        lp.horizontalBias = bias
        txtStreetName.layoutParams = lp
    }

    fun getPlaceholderLocation(placeholder: Placeholder): PointF {
        val view = getPlaceholderView(placeholder)
        return pointF.also {
            it.x = view.x + (view.width / 2f)
            it.y = view.y + (view.height / 2f)
        }
    }

    fun getPlaceholderView(placeholder: Placeholder) = when (placeholder) {
        Placeholder.CENTER -> placeholderCenterView
        Placeholder.ABOVE_STREET_NAME -> placeholderAboveStreetNameView
    }

    enum class Placeholder {
        CENTER,
        ABOVE_STREET_NAME,
    }

}

private fun calculateXBiasFromMeasuredWidth(viewWidth: Int, containerWidth: Int, bias: Float): Float {
    if (bias.isNaN() || bias < 0f || bias > 1f) {
        return 0.5f
    }

    val centerX = Math.round(containerWidth.toFloat() * bias)
    val viewLeft = centerX - Math.round(viewWidth.toFloat() / 2f)
    val viewRight = viewLeft + viewWidth

    val marginLeft = viewLeft
    val marginRight = containerWidth - viewRight
    val marginTotal = marginLeft + marginRight

    return if (marginTotal == 0) {
        0.5f
    } else {
        marginLeft.toFloat() / marginTotal.toFloat()
    }
}
