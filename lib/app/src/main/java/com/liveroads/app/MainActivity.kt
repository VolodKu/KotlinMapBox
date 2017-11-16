package com.liveroads.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.liveroads.app.search.SearchWorkFragment
import com.liveroads.common.log.obtainLogger
import com.liveroads.db.NavigationHistory
import com.liveroads.location.LocationProviderFragment
import com.liveroads.login.LoginActivity
import com.liveroads.util.findFragmentByTagOrAdd
import com.liveroads.util.log.onCreate
import com.liveroads.util.log.onDestroy
import com.google.firebase.auth.FirebaseUser



private const val REQUEST_CODE_LOGIN = 1
private const val TAG_LOCATION_PROVIDER_FRAGMENT = "liveroads.MainActivity.LocationProviderFragment"
private const val TAG_SEARCH_WORK_FRAGMENT = "liveroads.MainActivity.SearchWorkFragment"

class MainActivity : AppCompatActivity(), MainFragment.Listener {

    private val logger = obtainLogger()
    private var firebaseUser: FirebaseUser? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDb: FirebaseDatabase
    private lateinit var mainFragment: MainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.lr_activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDb = FirebaseDatabase.getInstance()

        val locationProviderFragment = supportFragmentManager.findFragmentByTagOrAdd(TAG_LOCATION_PROVIDER_FRAGMENT) {
            LocationProviderFragment()
        }

        val searchWorkFragment = supportFragmentManager.findFragmentByTagOrAdd(TAG_SEARCH_WORK_FRAGMENT) {
            SearchWorkFragment()
        }

        mainFragment = supportFragmentManager.findFragmentById(R.id.lr_activity_main_main_fragment) as MainFragment
        mainFragment.locationProviderFragment = locationProviderFragment
        mainFragment.searchWorkFragment = searchWorkFragment
        mainFragment.listener = this

        searchWorkFragment.locationProviderFragment = locationProviderFragment
    }

    override fun onDestroy() {
        logger.onDestroy()
        if (mainFragment.listener === this) {
            mainFragment.listener = null
        }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()

        if (firebaseAuth.currentUser == null) {
            startLoginActivity()
        }else{
            firebaseUser = firebaseAuth.currentUser

            // example using custom db module
            // add new record in navigation history
            var nh:NavigationHistory = NavigationHistory(this.applicationContext, firebaseDb,firebaseUser?.uid)

//            nh.addRecord("134 Thirtieth St. Etobicoke")

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_LOGIN) {
            if (resultCode != Activity.RESULT_OK) {
                finish()
            }else{
                firebaseUser = firebaseAuth.currentUser

                var nh:NavigationHistory = NavigationHistory(this.applicationContext, firebaseDb,firebaseUser?.uid)
//            nh.addRecord("123 Fake St.Canada")

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        val mainFragmentConsumedBackPressed = mainFragment.onBackPressed()
        if (!mainFragmentConsumedBackPressed) {
            super.onBackPressed()
        }
    }

    override fun onLogoutRequested(fragment: MainFragment) {
        logout()
    }

    private fun logout() {
        firebaseAuth.signOut()
        startLoginActivity()
    }

    private fun startLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            putExtra(LoginActivity.EXTRA_NEXT_INTENT, Intent(this@MainActivity, MainActivity::class.java))
        })
        finish()
    }

}
