package com.liveroads.ui

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.util.AttributeSet
import android.view.View


class SpeedLimitSignView : View {

    companion object {
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        private val speedDigitsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            isAntiAlias = true
            style = Paint.Style.FILL
            textAlign = Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        private val speedDimensionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            isAntiAlias = true
            style = Paint.Style.FILL
            textAlign = Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private var speedText: String = "60"
    private var dimensionText: String = "KM/H"

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        speedDigitsPaint.textSize = resources.getDimension(R.dimen.lr_speed_limit_sign_digits_text)
        speedDimensionPaint.textSize = resources.getDimension(R.dimen.lr_speed_limit_sign_dimension_text)
        borderPaint.strokeWidth = resources.getDimension(R.dimen.lr_speed_limit_sign_border)
    }

    fun update(speed: String, dimen: String) {
        speedText = speed
        dimensionText = dimen
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        drawBorder(canvas)
        drawSpeedNumbers(canvas)
        drawSpeedDimension(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val path = Path()
        path.addCircle(
                width/2.toFloat(),
                height/2.toFloat(),
                width.toFloat()/2.toFloat(),
                Path.Direction.CW
        )

        path.close()
        canvas.drawPath(path, bgPaint)
    }

    private fun drawBorder(canvas: Canvas) {
        val offset = borderPaint.strokeWidth/2
        canvas.drawCircle(
                (height/2).toFloat(),
                (height/2).toFloat(),
                (height/2).toFloat() - offset,
                borderPaint)
    }

    private fun drawSpeedNumbers(canvas: Canvas) {
        val xPos = (width / 2).toFloat()
        val yPos = (height / 2).toFloat() + height/8 - height/16

        canvas.drawText(speedText, xPos, yPos, speedDigitsPaint)
    }

    private fun drawSpeedDimension(canvas: Canvas) {
        val topOffset = (height / 2).toFloat() + height/8f

        val r = Rect()
        speedDimensionPaint.getTextBounds(dimensionText, 0, dimensionText.length, r)

        val xPos = (width / 2).toFloat()
        val yPos = topOffset + r.height()

        canvas.drawText(dimensionText, xPos, yPos, speedDimensionPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode != MeasureSpec.EXACTLY) {
            throw IllegalArgumentException("an exact width must be given")
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            throw IllegalArgumentException("an exact height must be given")
        }

        if (widthSize != heightSize) {
            throw IllegalArgumentException("width should be same as height")
        }

        setMeasuredDimension(widthSize, heightSize)
    }


}
