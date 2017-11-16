package com.liveroads.devtools

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liveroads.common.devtools.devTools
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.api.DevToolsService.LocationSource
import com.liveroads.devtools.client.monitor.*
import com.liveroads.devtools.main.R
import com.liveroads.util.log.*

private const val STYLE_URL_DAY_THEME = "mapbox://styles/liveroads/cj4yli0qd068n2rp7vu0md419"
private const val STYLE_URL_NIGHT_THEME = "mapbox://styles/liveroads/cj4ypxdlo001i2rn0bvjp27za"
private const val STYLE_URL_DAY_THEME_HD = "http://45.55.242.94/walid/lr/css/dayhd.json"
private const val STYLE_URL_NIGHT_THEME_HD = "http://45.55.242.94/walid/lr/css/nighthd.json"
private const val TAG_LARGE_MAP_STYLE_SELECT_DIALOG = "liveroads.DevToolsFragment.TAG_LARGE_MAP_STYLE_SELECT_DIALOG"
private const val TAG_PIP_MAP_STYLE_SELECT_DIALOG = "liveroads.DevToolsFragment.TAG_PIP_MAP_STYLE_SELECT_DIALOG"
private const val TAG_LOCATION_SOURCE = "liveroads.DevToolsFragment.TAG_LOCATION_SOURCE"

class DevToolsFragment :
        Fragment(),
        MapStyleSelectDialogFragment.Listener,
        LocationSourceSelectDialogFragment.Listener {

    private val logger = obtainLogger()

    private val spinnerView: View
        get() = view!!.findViewById(R.id.lr_fragment_devtools_spinner)
    private val recyclerView: RecyclerView
        get() = view!!.findViewById(R.id.lr_fragment_devtools_recycler_view)

    private val devToolsSettingMonitorListener = object : DevToolsSettingMonitor.Listener {
        override fun onValueChanged(monitor: DevToolsSettingMonitor<*>) {
            updateUi()
        }
    }

    private val devToolsEnabledMonitor = DevToolsEnabledMonitor(devTools).apply {
        addListener(devToolsSettingMonitorListener)
    }
    private val mapTiltZoomEnabledMonitor = MapTiltZoomEnabledMonitor(devTools).apply {
        addListener(devToolsSettingMonitorListener)
    }
    private val largeMapStyleUrlMonitor = LargeMapStyleUrlMonitor(devTools).apply {
        addListener(devToolsSettingMonitorListener)
    }
    private val pipMapStyleUrlMonitor = PipMapStyleUrlMonitor(devTools).apply {
        addListener(devToolsSettingMonitorListener)
    }
    private val locationSourceMonitor = LocationSourceMonitor(devTools).apply {
        addListener(devToolsSettingMonitorListener)
    }
    private val largeMapCenterAsLocationEnabledMonitor = LargeMapCenterAsLocationEnabledMonitor(devTools).apply {
        addListener(devToolsSettingMonitorListener)
    }
    private val mapGridLinesEnabledMonitor = MapGridLinesEnabledMonitor(devTools).apply {
        addListener(devToolsSettingMonitorListener)
    }

    private lateinit var adapter: DevToolsSettingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logger.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.lr_fragment_devtools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        logger.onViewCreated(view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        adapter = DevToolsSettingAdapter(settings, recyclerViewListener)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.itemAnimator = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
        fragmentManager.findFragmentByTag(TAG_LARGE_MAP_STYLE_SELECT_DIALOG)?.let {
            (it as MapStyleSelectDialogFragment).listener = this
        }
        fragmentManager.findFragmentByTag(TAG_PIP_MAP_STYLE_SELECT_DIALOG)?.let {
            (it as MapStyleSelectDialogFragment).listener = this
        }
        fragmentManager.findFragmentByTag(TAG_LOCATION_SOURCE)?.let {
            (it as LocationSourceSelectDialogFragment).listener = this
        }
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()

        devToolsEnabledMonitor.start()
        mapTiltZoomEnabledMonitor.start()
        largeMapStyleUrlMonitor.start()
        pipMapStyleUrlMonitor.start()
        locationSourceMonitor.start()
        largeMapCenterAsLocationEnabledMonitor.start()
        mapGridLinesEnabledMonitor.start()
        updateUi()
    }

    override fun onStop() {
        logger.onStop()
        mapGridLinesEnabledMonitor.stop()
        largeMapCenterAsLocationEnabledMonitor.stop()
        locationSourceMonitor.stop()
        pipMapStyleUrlMonitor.stop()
        largeMapStyleUrlMonitor.stop()
        mapTiltZoomEnabledMonitor.stop()
        devToolsEnabledMonitor.stop()
        super.onStop()
    }

    private fun updateUi() {
        spinnerView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        devToolsEnabledSetting.value = getEnabledText(devToolsEnabledMonitor.value)
        mapTiltZoomVisibleSetting.value = getVisibleText(mapTiltZoomEnabledMonitor.value)
        largeMapStyleSetting.value = getMapStyleText(largeMapStyleUrlMonitor.value)
        pipMapStyleSetting.value = getMapStyleText(pipMapStyleUrlMonitor.value)
        locationSourceSetting.value = getLocationSourceText(locationSourceMonitor.value)
        largeMapCenterAsLocationSetting.value = getEnabledText(largeMapCenterAsLocationEnabledMonitor.value)
        mapGridLinesEnabledSetting.value = getEnabledText(mapGridLinesEnabledMonitor.value)

        adapter.isDevToolsEnabled = devToolsEnabledMonitor.value
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    private fun getVisibleText(visible: Boolean) = if (visible) {
        getText(R.string.lr_devtools_visible)
    } else {
        getText(R.string.lr_devtools_hidden)
    }

    private fun getEnabledText(enabled: Boolean) = if (enabled) {
        getText(R.string.lr_devtools_enabled)
    } else {
        getText(R.string.lr_devtools_disabled)
    }

    private fun getMapStyleText(styleUrl: String?): CharSequence {
        return when (styleUrl) {
            null -> getText(R.string.lr_devtools_default_value)
            STYLE_URL_DAY_THEME -> getText(R.string.lr_devtools_day_theme)
            STYLE_URL_NIGHT_THEME -> getText(R.string.lr_devtools_night_theme)
            STYLE_URL_DAY_THEME_HD -> getText(R.string.lr_devtools_day_theme_hd)
            STYLE_URL_NIGHT_THEME_HD -> getText(R.string.lr_devtools_night_theme_hd)
            else -> styleUrl
        }
    }

    private fun getLocationSourceText(value: LocationSource?): CharSequence {
        return when (value) {
            null -> getText(R.string.lr_devtools_default_value)
            LocationSource.BLACKBOX -> getText(R.string.lr_devtools_map_source_blackbox)
            LocationSource.GMS -> getText(R.string.lr_devtools_map_source_gms)
        }
    }

    private fun askUserSelectMapStyle(tag: String, @StringRes titleResId: Int) {
        val fragment = MapStyleSelectDialogFragment()
        fragment.listener = this
        fragment.arguments = Bundle().apply {
            putInt(MapStyleSelectDialogFragment.ARG_TITLE_RES_ID, titleResId)
        }
        fragment.show(fragmentManager, tag)
    }

    private fun askUserSelectLocationSource(@StringRes titleResId: Int) {
        val fragment = LocationSourceSelectDialogFragment()
        fragment.listener = this
        fragment.arguments = Bundle().apply {
            putInt(LocationSourceSelectDialogFragment.ARG_TITLE_RES_ID, titleResId)
        }
        fragment.show(fragmentManager, TAG_LOCATION_SOURCE)
    }

    override fun onSelected(fragment: MapStyleSelectDialogFragment, choice: MapStyleSelectDialogFragment.Choice) {
        val key = when (fragment.tag) {
            TAG_LARGE_MAP_STYLE_SELECT_DIALOG -> largeMapStyleUrlMonitor.key
            TAG_PIP_MAP_STYLE_SELECT_DIALOG -> pipMapStyleUrlMonitor.key
            else -> throw IllegalArgumentException("unknown fragment: $fragment")
        }
        val value = when (choice) {
            MapStyleSelectDialogFragment.Choice.DEFAULT -> null
            MapStyleSelectDialogFragment.Choice.DAY_THEME -> STYLE_URL_DAY_THEME
            MapStyleSelectDialogFragment.Choice.NIGHT_THEME -> STYLE_URL_NIGHT_THEME
            MapStyleSelectDialogFragment.Choice.DAY_THEME_HD -> STYLE_URL_DAY_THEME_HD
            MapStyleSelectDialogFragment.Choice.NIGHT_THEME_HD -> STYLE_URL_NIGHT_THEME_HD
        }
        devTools.setStringSetting(key, value)
    }

    override fun onSelected(fragment: LocationSourceSelectDialogFragment, choice: LocationSourceSelectDialogFragment.Choice) {
        val value = when (choice) {
            LocationSourceSelectDialogFragment.Choice.DEFAULT -> null
            LocationSourceSelectDialogFragment.Choice.BLACK_BOX -> LocationSource.BLACKBOX
            LocationSourceSelectDialogFragment.Choice.GMS -> LocationSource.GMS
        }
        devTools.setStringSetting(LocationSourceMonitor.KEY, value?.name)
    }

    private fun onSettingClick(setting: DevToolsSetting) {
        if (setting === devToolsEnabledSetting) {
            val oldValue = devToolsEnabledMonitor.value
            val newValue = !oldValue
            devTools.isEnabled = newValue
        } else if (setting === mapTiltZoomVisibleSetting) {
            val oldValue = mapTiltZoomEnabledMonitor.value
            val newValue = !oldValue
            devTools.setBooleanSetting(mapTiltZoomEnabledMonitor.key, newValue)
        } else if (setting === largeMapStyleSetting) {
            askUserSelectMapStyle(TAG_LARGE_MAP_STYLE_SELECT_DIALOG, setting.titleStringResId)
        } else if (setting === pipMapStyleSetting) {
            askUserSelectMapStyle(TAG_PIP_MAP_STYLE_SELECT_DIALOG, setting.titleStringResId)
        } else if (setting === locationSourceSetting) {
            askUserSelectLocationSource(locationSourceSetting.titleStringResId)
        } else if (setting === largeMapCenterAsLocationSetting) {
            val oldValue = largeMapCenterAsLocationEnabledMonitor.value
            val newValue = !oldValue
            devTools.setBooleanSetting(largeMapCenterAsLocationEnabledMonitor.key, newValue)
        } else if (setting === mapGridLinesEnabledSetting) {
            val oldValue = mapGridLinesEnabledMonitor.value
            val newValue = !oldValue
            devTools.setBooleanSetting(mapGridLinesEnabledMonitor.key, newValue)
        } else {
            throw IllegalArgumentException("unknown setting: $setting")
        }
    }

    private data class DevToolsSetting(
            @StringRes val titleStringResId: Int,
            var value: CharSequence?,
            @DrawableRes val iconResId: Int
    )

    private val devToolsEnabledSetting = DevToolsSetting(
            titleStringResId = R.string.lr_devtools_enabled_checkbox_label,
            value = null,
            iconResId = 0
    )

    private val mapTiltZoomVisibleSetting = DevToolsSetting(
            titleStringResId = R.string.lr_devtools_map_tilt_zoom_visible_checkbox_label,
            value = null,
            iconResId = 0
    )

    private val largeMapStyleSetting = DevToolsSetting(
            titleStringResId = R.string.lr_devtools_large_map_style_title,
            value = null,
            iconResId = 0
    )

    private val pipMapStyleSetting = DevToolsSetting(
            titleStringResId = R.string.lr_devtools_pip_map_style_title,
            value = null,
            iconResId = 0
    )

    private val locationSourceSetting = DevToolsSetting(
            titleStringResId = R.string.lr_devtools_location_source_title,
            value = null,
            iconResId = 0
    )

    private val largeMapCenterAsLocationSetting = DevToolsSetting(
            titleStringResId = R.string.lr_devtools_large_map_center_as_location_title,
            value = null,
            iconResId = 0
    )

    private val mapGridLinesEnabledSetting = DevToolsSetting(
            titleStringResId = R.string.lr_devtools_map_grid_lines_enabled_title,
            value = null,
            iconResId = 0
    )

    private val settings = mutableListOf<DevToolsSetting>(
            devToolsEnabledSetting,
            mapTiltZoomVisibleSetting,
            largeMapStyleSetting,
            pipMapStyleSetting,
            locationSourceSetting,
            largeMapCenterAsLocationSetting,
            mapGridLinesEnabledSetting)

    private val recyclerViewListener = object : DevToolsSettingViewHolder.Listener {
        override fun onClick(vh: DevToolsSettingViewHolder) {
            onSettingClick(vh.data!!)
        }
    }

    private class DevToolsSettingAdapter(val items: List<DevToolsSetting>,
            val listener: DevToolsSettingViewHolder.Listener)
        : RecyclerView.Adapter<DevToolsSettingViewHolder>() {

        var isDevToolsEnabled: Boolean = false
            set(enabled) {
                if (field != enabled) {
                    field = enabled
                    if (enabled) {
                        notifyItemRangeInserted(1, items.size - 1)
                    } else {
                        notifyItemRangeRemoved(1, items.size - 1)
                    }
                }
            }

        override fun getItemCount() = if (isDevToolsEnabled) items.size else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevToolsSettingViewHolder {
            val vh = DevToolsSettingViewHolder(parent)
            vh.listener = listener
            return vh
        }

        override fun onBindViewHolder(vh: DevToolsSettingViewHolder, position: Int) {
            vh.data = items[position]
        }

        override fun onViewRecycled(vh: DevToolsSettingViewHolder) {
            vh.data = null
        }

    }

    private class DevToolsSettingViewHolder(val view: DevToolsSettingView) : ViewHolder(view), View.OnClickListener {

        constructor(parent: ViewGroup) : this(inflate(parent))

        var listener: Listener? = null

        init {
            view.setOnClickListener(this)
        }

        var data: DevToolsSetting? = null
            get() = field
            set(value) {
                field = value
                if (value == null) {
                    view.titleView.text = null
                    view.subtitleView.text = null
                    view.iconView.setImageDrawable(null)
                } else {
                    view.titleView.setText(value.titleStringResId)
                    view.subtitleView.setText(value.value)
                    if (value.iconResId == 0) {
                        view.iconView.setImageDrawable(null)
                        view.iconView.visibility = View.GONE
                    } else {
                        view.iconView.setImageResource(value.iconResId)
                        view.iconView.visibility = View.VISIBLE
                    }
                }
            }

        companion object {

            fun inflate(parent: ViewGroup) = LayoutInflater.from(parent.context).let {
                it.inflate(R.layout.lr_fragment_devtools_entry, parent, false) as DevToolsSettingView
            }

        }

        override fun onClick(view: View?) {
            listener?.onClick(this)
        }

        interface Listener {

            fun onClick(vh: DevToolsSettingViewHolder)

        }

    }

}
