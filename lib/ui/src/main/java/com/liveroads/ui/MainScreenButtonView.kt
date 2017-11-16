package com.liveroads.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import com.liveroads.util.findViewByIdOrThrow
import com.liveroads.util.use

class MainScreenButtonView : FrameLayout {

    val imageView: ImageView

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

    init {
        inflate(context, R.layout.lr_view_main_screen_button, this)
        imageView = findViewByIdOrThrow(R.id.image)
        isClickable = true
        isFocusable = true
    }

    fun applyAttributes(attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LiveRoadsUI, defStyleAttr, defStyleRes).use {
            if (hasValue(R.styleable.LiveRoadsUI_lr_contentDescription)) {
                imageView.contentDescription = getText(R.styleable.LiveRoadsUI_lr_contentDescription)
            }
            if (hasValue(R.styleable.LiveRoadsUI_lr_src)) {
                imageView.setImageResource(getResourceId(R.styleable.LiveRoadsUI_lr_src, 0))
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?) = true

}
