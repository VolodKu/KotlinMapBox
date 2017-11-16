package com.liveroads.devtools

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.liveroads.devtools.main.R
import com.liveroads.util.findViewByIdOrThrow
import com.liveroads.util.use

class DevToolsSettingView : FrameLayout {

    val titleView: TextView
    val subtitleView: TextView
    val iconView: ImageView

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
        inflate(context, R.layout.preference_material, this)
        titleView = findViewByIdOrThrow(android.R.id.title)
        subtitleView = findViewByIdOrThrow(android.R.id.summary)
        iconView = findViewByIdOrThrow(android.R.id.icon)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?) = true

    fun applyAttributes(attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LiveRoadsDevToolsUI, defStyleAttr, defStyleRes).use {
            if (hasValue(R.styleable.LiveRoadsDevToolsUI_lr_devtools_title)) {
                titleView.text = getText(R.styleable.LiveRoadsDevToolsUI_lr_devtools_title)
            }
            if (hasValue(R.styleable.LiveRoadsDevToolsUI_lr_devtools_subtitle)) {
                subtitleView.text = getText(R.styleable.LiveRoadsDevToolsUI_lr_devtools_subtitle)
            }
            if (hasValue(R.styleable.LiveRoadsDevToolsUI_lr_devtools_icon)) {
                iconView.setImageDrawable(getDrawable(R.styleable.LiveRoadsDevToolsUI_lr_devtools_icon))
            }
            if (hasValue(R.styleable.LiveRoadsDevToolsUI_lr_devtools_icon_alpha)) {
                iconView.alpha = getFloat(R.styleable.LiveRoadsDevToolsUI_lr_devtools_icon, iconView.alpha)
            }
        }
    }

}
