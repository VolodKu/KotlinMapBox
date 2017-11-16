package com.liveroads.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.liveroads.util.use

class GridLinesView : View {

    private val linePaint1: Paint
    private val linePaint2: Paint
    private val linePaint3: Paint

    private val lines1 = List<RectF>(2) { RectF() }
    private val lines2 = List<RectF>(4) { RectF() }
    private val lines3 = List<RectF>(8) { RectF() }

    constructor(context: Context)
            : super(context) {
        applyAttributes()
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

    fun applyAttributes(attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LiveRoadsUI, defStyleAttr, defStyleRes).use {
        }
    }

    init {
        linePaint1 = Paint()
        linePaint1.strokeWidth = 3.0f
        linePaint1.color = Color.RED
        linePaint1.style = Paint.Style.STROKE
        linePaint2 = Paint(linePaint1)
        linePaint2.strokeWidth = 2.0f
        linePaint2.alpha = 192
        linePaint3 = Paint(linePaint1)
        linePaint3.strokeWidth = 1.0f
        linePaint3.alpha = 128
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

        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = (right - left).toFloat()
        val height = (bottom - top).toFloat()
        lines1[0].let {
            it.left = 0f
            it.right = width
            it.top = height / 2f
            it.bottom = it.top
        }
        lines1[1].let {
            it.left = width / 2f
            it.right = it.left
            it.top = 0f
            it.bottom = height
        }

        lines2[0].let {
            it.left = 0f
            it.right = width
            it.top = height / 4f
            it.bottom = it.top
        }
        lines2[1].let {
            it.left = 0f
            it.right = width
            it.top = (height * 3f) / 4f
            it.bottom = it.top
        }
        lines2[2].let {
            it.left = width / 4f
            it.right = it.left
            it.top = 0f
            it.bottom = height
        }
        lines2[3].let {
            it.left = (width * 3f) / 4f
            it.right = it.left
            it.top = 0f
            it.bottom = height
        }

        lines3[0].let {
            it.left = 0f
            it.right = width
            it.top = height / 8f
            it.bottom = it.top
        }
        lines3[1].let {
            it.left = 0f
            it.right = width
            it.top = (height * 3f) / 8f
            it.bottom = it.top
        }
        lines3[2].let {
            it.left = 0f
            it.right = width
            it.top = (height * 5f) / 8f
            it.bottom = it.top
        }
        lines3[3].let {
            it.left = 0f
            it.right = width
            it.top = (height * 7f) / 8f
            it.bottom = it.top
        }
        lines3[4].let {
            it.left = width / 8f
            it.right = it.left
            it.top = 0f
            it.bottom = height
        }
        lines3[5].let {
            it.left = (width * 3f) / 8f
            it.right = it.left
            it.top = 0f
            it.bottom = height
        }
        lines3[6].let {
            it.left = (width * 5f) / 8f
            it.right = it.left
            it.top = 0f
            it.bottom = height
        }
        lines3[7].let {
            it.left = (width * 7f) / 8f
            it.right = it.left
            it.top = 0f
            it.bottom = height
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        lines1.forEach {
            canvas.drawLine(it.left, it.top, it.right, it.bottom, linePaint1)
        }
        lines2.forEach {
            canvas.drawLine(it.left, it.top, it.right, it.bottom, linePaint2)
        }
        lines3.forEach {
            canvas.drawLine(it.left, it.top, it.right, it.bottom, linePaint3)
        }
    }


}
