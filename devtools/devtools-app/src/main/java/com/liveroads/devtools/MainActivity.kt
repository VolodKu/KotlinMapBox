package com.liveroads.devtools

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent().apply {
            setClassName("com.liveroads", "com.liveroads.devtools.DevToolsActivity")
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Live Roads is not installed", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

}
