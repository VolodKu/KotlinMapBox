package com.liveroads.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.liveroads.util.use

class AnimatedButton : View {

    var drawable: Drawable? = null
        set(value) {
            field = value
            requestLayout()
        }

    var clickAlpha: Float = Float.NaN
        set(value) {
            field = value
            requestLayout()
        }

    var growthFactor = Float.NaN
        set(value) {
            field = value
            requestLayout()
        }

    private var clicked = false
        set(value) {
            if (field != value) {
                if (field != value) {
                    field = value
                    startClickAnimation()
                    requestLayout()
                }
            }
        }

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
            if (hasValue(R.styleable.LiveRoadsUI_lr_src)) {
                drawable = getDrawable(R.styleable.LiveRoadsUI_lr_src)
            }
            if (hasValue(R.styleable.LiveRoadsUI_lr_click_alpha)) {
                clickAlpha = getFloat(R.styleable.LiveRoadsUI_lr_click_alpha, clickAlpha)
            }
            if (hasValue(R.styleable.LiveRoadsUI_lr_growth_factor)) {
                growthFactor = getFloat(R.styleable.LiveRoadsUI_lr_growth_factor, growthFactor)
            }
        }
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

        val drawable = this.drawable ?: return

        val availableWidth = right - left - paddingLeft - paddingRight
        val availableHeight = bottom - top - paddingTop - paddingBottom

        val drawableWidth: Int
        val drawableHeight: Int
        if (availableWidth < 0 || availableHeight < 0) {
            drawableWidth = 0
            drawableHeight = 0
        } else if (growthFactor.isNaN() || growthFactor == 0f) {
            drawableWidth = availableWidth
            drawableHeight = availableHeight
        } else {
            val clickedWidth = availableWidth
            val clickedHeight = availableHeight
            val unclickedWidth = Math.round(availableWidth / growthFactor)
            val unclickedHeight = Math.round(availableHeight / growthFactor)

            if (clickAnimator.isRunning) {
                drawableWidth = unclickedWidth + Math.round((clickedWidth - unclickedWidth) * clickAnimator.animatedFraction)
                drawableHeight = unclickedHeight + Math.round((clickedHeight - unclickedHeight) * clickAnimator.animatedFraction)
            } else if (clicked) {
                drawableWidth = clickedWidth
                drawableHeight = clickedHeight
            } else {
                drawableWidth = unclickedWidth
                drawableHeight = unclickedHeight
            }
        }

        val drawableLeft = paddingLeft + Math.round(availableWidth / 2f) - Math.round(drawableWidth / 2f)
        val drawableRight = drawableLeft + drawableWidth
        val drawableTop = paddingTop + Math.round(availableHeight / 2f) - Math.round(drawableHeight / 2f)
        val drawableBottom = drawableTop + drawableHeight

        drawable.setBounds(drawableLeft, drawableTop, drawableRight, drawableBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawable?.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.getActionMasked()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (isClickable) {
                    clicked = true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                clicked = false
            }
        }

        return super.onTouchEvent(event)
    }

    private fun startClickAnimation() {
        if (!isAttachedToWindow) {
            return
        }

        if (clickAnimator.isRunning) {
            clickAnimator.reverse()
        } else if (clicked) {
            if (clickAnimator.unclickAlpha.isNaN()) {
                clickAnimator.unclickAlpha = alpha
            }
            clickAnimator.start()
        } else {
            clickAnimator.reverse()
        }
    }

    override fun onDetachedFromWindow() {
        clickAnimator.cancel()
        super.onDetachedFromWindow()
    }

    private val clickAnimator = object : ValueAnimator(), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

        var unclickAlpha: Float = Float.NaN

        init {
            setFloatValues(0f, 1f)
            duration = 300L
            interpolator = OvershootInterpolator()
            addUpdateListener(this)
            addListener(this)
        }

        override fun onAnimationUpdate(animator: ValueAnimator) {
            if (!unclickAlpha.isNaN() && !clickAlpha.isNaN()) {
                val alphaDiff = clickAlpha - unclickAlpha
                val newAlpha = unclickAlpha + (animator.animatedFraction * alphaDiff)
                alpha = newAlpha
            }
            requestLayout()
        }

        override fun onAnimationStart(animator: Animator) {
        }

        override fun onAnimationEnd(animator: Animator) {
        }

        override fun onAnimationRepeat(animator: Animator) {
        }

        override fun onAnimationCancel(animator: Animator) {
        }

    }

}
