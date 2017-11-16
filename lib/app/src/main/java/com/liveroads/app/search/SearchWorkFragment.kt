package com.liveroads.app.search

import android.os.*
import android.support.annotation.MainThread
import android.support.v4.app.Fragment
import com.liveroads.common.executors.Executors
import com.liveroads.common.log.obtainLogger
import com.liveroads.location.LocationProviderFragment
import com.liveroads.util.log.logLifecycle
import com.liveroads.util.log.onCreate
import com.liveroads.util.log.onDestroy
import java.lang.ref.WeakReference

class SearchWorkFragment : Fragment() {

    private val logger = obtainLogger()

    private lateinit var selfRef: WeakReference<SearchWorkFragment>
    private lateinit var mainHandler: MainHandler
    private lateinit var listeners: MutableList<Listener>

    val isSearchInProgress: Boolean
        get() = (searchTask != null)
    var searchResult: Any? = null
        private set
    var stateId: IBinder = Binder()
        private set
    lateinit var locationProviderFragment: LocationProviderFragment

    private var searchType : SearchType = SearchType.SEARCH_AUTOCOMPLETE

    private var searchTask: SearchTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        retainInstance = true

        selfRef = WeakReference(this)
        mainHandler = MainHandler(selfRef)
        listeners = mutableListOf()
    }

    override fun onDestroy() {
        logger.onDestroy()
        searchTask?.cancel(true)
        listeners.clear()
        mainHandler.removeCallbacksAndMessages(null)
        selfRef.clear()
        super.onDestroy()
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        for (i in listeners.size - 1 downTo 0) {
            if (listeners[i] === listener) {
                listeners.removeAt(i)
                break
            }
        }
    }

    fun setSearchType(type: SearchType) {
        searchType = type;
    }

    fun startSearchDelayed(text: String) {
        mainHandler.removeMessages(MainHandler.Op.START_SEARCH.ordinal)
        searchTask?.cancel(true)
        searchTask = null

        val message = mainHandler.obtainMessage()
        message.what = MainHandler.Op.START_SEARCH.ordinal
        message.obj = text
        mainHandler.sendMessageDelayed(message, SEARCH_START_DELAY_MILLIS)
    }

    fun startSearch(text: String) {
        logger.logLifecycle("startSearch() text=%s", text)
        mainHandler.removeMessages(MainHandler.Op.START_SEARCH.ordinal)
        searchTask?.cancel(true)
        val latitude = locationProviderFragment.location?.latitude ?: Double.NaN
        val longitude = locationProviderFragment.location?.longitude ?: Double.NaN

        searchTask = SearchTask(selfRef, text, latitude, longitude, searchType, "","").apply {
            executeOnExecutor(Executors.NORMAL_PRIORITY_IMMEDIATE)
        }
        handleStateChanged()
    }

    fun startPOISearch(type:String, values: String) {
        logger.logLifecycle("startSearch() text=%s", values)
        mainHandler.removeMessages(MainHandler.Op.START_SEARCH.ordinal)
        searchTask?.cancel(true)
        var latitude = locationProviderFragment.location?.latitude ?: Double.NaN
        var longitude = locationProviderFragment.location?.longitude ?: Double.NaN

        //For Testing POI
        longitude = -79.531867
        latitude = 43.600142
        //end

        searchTask = SearchTask(selfRef, "", latitude, longitude, searchType, type, values).apply {
            executeOnExecutor(Executors.NORMAL_PRIORITY_IMMEDIATE)
        }
        handleStateChanged()
    }

    fun stopSearch() {
        mainHandler.removeMessages(MainHandler.Op.START_SEARCH.ordinal)
        searchTask?.cancel(true)
        searchTask = null
        handleStateChanged()
    }

    fun clearSearchResult() {
        searchResult = null
        handleStateChanged()
    }

    internal fun onSearchTaskComplete(task: SearchTask, result: Any) {
        logger.logLifecycle("onSearchTaskComplete()")
        if (task !== searchTask) {
            throw IllegalArgumentException("incorrect SearchTask")
        }
        searchTask = null
        searchResult = result
        handleStateChanged()
    }

    private fun handleStateChanged() {
        stateId = Binder()
        listeners.forEach { it.onStateChanged(this) }
    }

    interface Listener {

        fun onStateChanged(fragment: SearchWorkFragment)

    }

    @MainThread
    private class MainHandler(val fragment: WeakReference<SearchWorkFragment>) : Handler() {

        enum class Op {
            START_SEARCH,
        }

        override fun handleMessage(msg: Message) {
            val op = Op.values()[msg.what]
            when (op) {
                Op.START_SEARCH -> fragment.get()?.startSearch(msg.obj as String)
            }
        }

    }

    companion object {
        const val SEARCH_START_DELAY_MILLIS = 25L
    }

    enum class SearchType {
        SEARCH_AUTOCOMPLETE,
        SEARCH_ESTABLISHMENT,
        SEARCH_YELP
    }
}
