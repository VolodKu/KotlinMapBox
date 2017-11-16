package com.liveroads.app.search

/**
 * Created by android on 8/25/17.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.liveroads.app.MainFragment
import com.liveroads.app.R
import com.liveroads.common.log.obtainLogger
import com.liveroads.db.NavigationItem
import com.liveroads.db.UserAddress
import com.liveroads.mapzen.body.AutocompleteResponse
import com.liveroads.ui.search.SearchResultEntryView
import com.liveroads.util.findViewByIdOrThrow
import com.liveroads.util.log.*
import com.mapbox.mapboxsdk.geometry.LatLng

class SearchHomeFragment: Fragment(), View.OnClickListener {

    val logger = obtainLogger()

    var homeMenuSection : LinearLayout? = null
    var workMenuSection : LinearLayout? = null
    var homeSection : LinearLayout? = null
    var workSection : LinearLayout? = null
    var homeAddress : TextView? = null
    var workAddress : TextView? = null
    var homeDistance : TextView? = null
    var workDistance : TextView? = null

    var recentShow : RelativeLayout? = null
    var recentSearch : SearchResultEntryView? = null

    var listener: Listener? = null

    private lateinit var userAddress:UserAddress
    private var home_address: NavigationItem? = null
    private var work_address: NavigationItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        userAddress = UserAddress(this.context, FirebaseDatabase.getInstance(), FirebaseAuth.getInstance().currentUser?.uid)
    }

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {

            var action = intent?.action
            if (action.equals(MainFragment.ACTION_RECENT_SEARCHING_HISTORY_CHANGE))
            {
                var history:ArrayList<NavigationItem> = MainFragment.getHistory()

                if (history.size > 0)
                {
                    recentSearch!!.visibility = View.VISIBLE
                    recentSearch!!.titleView.text = history.get(history.size - 1).location
                    recentSearch!!.subtitleView.text = history.get(history.size - 1).province
                    recentSearch!!.distance.text = java.lang.String.format("%.2fKm", history.get(history.size - 1).distance)
                }
            }
            else if (action.equals(MainFragment.ACTION_HOME_WORK_ADDRESS_CHANGE))
            {
                home_address = userAddress.homeAddress
                work_address = userAddress.wordAddress

                if (home_address != null)
                {
                    homeMenuSection!!.visibility = View.INVISIBLE
                    homeSection!!.visibility = View.VISIBLE

                    homeAddress!!.text = home_address?.location

                }
                if (work_address != null)
                {
                    workMenuSection!!.visibility = View.INVISIBLE
                    workSection!!.visibility = View.VISIBLE

                    workAddress!!.text = home_address?.location
                }

                updateHomeWorkDistance()
            }
            else if (action.equals(MainFragment.ACTION_USER_LOCATION_CHANGE))
            {
                updateHomeWorkDistance()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logger.onCreateView(inflater, container, savedInstanceState)

        val view : View = inflater.inflate(R.layout.lr_search_home_fragment, container, false)
        homeMenuSection = view.findViewByIdOrThrow<LinearLayout>(R.id.home_address_menu)
        workMenuSection = view.findViewByIdOrThrow(R.id.work_address_menu)
        homeSection = view.findViewByIdOrThrow(R.id.set_home_address)
        workSection = view.findViewByIdOrThrow(R.id.set_work_address)
        homeAddress = view.findViewByIdOrThrow(R.id.home_address)
        workAddress = view.findViewByIdOrThrow(R.id.work_address)
        homeDistance = view.findViewByIdOrThrow(R.id.home_distance)
        workDistance = view.findViewByIdOrThrow(R.id.work_distance)

        recentShow = view.findViewByIdOrThrow(R.id.lr_recent)
        recentSearch = view.findViewByIdOrThrow(R.id.lr_recentSearch)

        var history:ArrayList<NavigationItem> = MainFragment.getHistory()

        if (history.size > 0)
        {
            recentSearch!!.visibility = View.VISIBLE
            recentSearch!!.titleView.text = history.get(history.size - 1).location
            recentSearch!!.subtitleView.text = history.get(history.size - 1).province
            recentSearch!!.distance.text = java.lang.String.format("%.2fKm", history.get(history.size - 1).distance)
        }

        recentSearch!!.setOnClickListener(this)

        homeMenuSection!!.setOnClickListener(this)
        workMenuSection!!.setOnClickListener(this)
        homeSection!!.setOnClickListener(this)
        workSection!!.setOnClickListener(this)

        var intentFilter = IntentFilter(MainFragment.ACTION_RECENT_SEARCHING_HISTORY_CHANGE)
        intentFilter.addAction(MainFragment.ACTION_HOME_WORK_ADDRESS_CHANGE)
        intentFilter.addAction(MainFragment.ACTION_USER_LOCATION_CHANGE)

        LocalBroadcastManager.getInstance(this.context).registerReceiver(broadCastReceiver, intentFilter)

        return view
    }

    fun updateHomeWorkDistance()
    {
        if (MainFragment.instanceObj.mainMapsFragment.largeMapFragment.mapLocation == null) return

        var mapLocation = LatLng(MainFragment.instanceObj.mainMapsFragment.largeMapFragment.mapLocation!!.latitude,
                MainFragment.instanceObj.mainMapsFragment.largeMapFragment.mapLocation!!.longitude)

        if (home_address != null)
        {
            var homeLocation = LatLng(home_address?.latitude!!,
                    home_address?.longitude!!)

            homeDistance!!.text = getDistanceString(homeLocation.distanceTo(mapLocation))
        }

        if (work_address != null)
        {
            var workLocation = LatLng(work_address?.latitude!!,
                    work_address?.longitude!!)

            workDistance!!.text = getDistanceString(workLocation.distanceTo(mapLocation))
        }
    }

    fun getDistanceString(distance:Double):String{
        if (distance < 1000)
        {
            return java.lang.String.format("%.2f m", distance)
        }
        return java.lang.String.format("%.2f km", distance/1000)
    }

    override fun onClick(p0: View?) {

        if (p0!!.id == R.id.lr_recentSearch)
        {
            var history:ArrayList<NavigationItem> = MainFragment.getHistory()

            if (history.size == 0) return

            listener?.onSearchResultSelected(this, history.get(history.size - 1))
        }
        else if (p0!!.id == R.id.home_address_menu || p0!!.id == R.id.set_home_address)
        {
            userAddress.addRecord("home", MainFragment.instanceObj.mainMapsFragment.largeMapFragment.streetName, "",
                    MainFragment.instanceObj.mainMapsFragment.largeMapFragment.cameraLocation!!.latitude,
                    MainFragment.instanceObj.mainMapsFragment.largeMapFragment.cameraLocation!!.longitude,
                    0.0)
        }
        else if (p0!!.id == R.id.work_address_menu || p0!!.id == R.id.set_work_address)
        {
            userAddress.addRecord("work", MainFragment.instanceObj.mainMapsFragment.largeMapFragment.streetName, "",
                    MainFragment.instanceObj.mainMapsFragment.largeMapFragment.cameraLocation!!.latitude,
                    MainFragment.instanceObj.mainMapsFragment.largeMapFragment.cameraLocation!!.longitude,
                    0.0)
        }


    }

    override fun onDestroy() {
        logger.onDestroy()
        LocalBroadcastManager.getInstance(this.context).unregisterReceiver(broadCastReceiver)
        super.onDestroy()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        logger.onActivityCreated(savedInstanceState)
        super.onActivityCreated(savedInstanceState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        logger.onViewStateRestored(savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onStart() {
        logger.onStart()
        super.onStart()
    }

    override fun onStop() {
        logger.onStop()
        super.onStop()
    }

    interface Listener {

        fun onSearchResultSelected(fragment: SearchHomeFragment, selectedResult: NavigationItem)

    }
}

