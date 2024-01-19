package com.example.eldergram

import android.annotation.SuppressLint
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.system.exitProcess

const val APP_NAME = "eldergram"

class MainActivity : ComponentActivity() {
    class MyDeviceAdminReceiver: DeviceAdminReceiver()
    private val mCountDownTimer = object : CountDownTimer(5000, 5500) {
        override fun onFinish() {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        override fun onTick(millisUntilFinished: Long) {
            TODO("Not yet implemented")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contacts_list)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val tv = findViewById<TextView>(R.id.battery_info)
                var batteryCharge = 0
                if (intent != null) {
                    batteryCharge = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                }
                tv.text = getString(R.string.battery_info).format(batteryCharge)
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val tv = findViewById<TextView>(R.id.battery_info)
                tv.setTextColor(Color.RED)
            }
        }, IntentFilter(Intent.ACTION_BATTERY_LOW))
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val tv = findViewById<TextView>(R.id.battery_info)
                tv.setTextColor(Color.WHITE)
            }
        }, IntentFilter(Intent.ACTION_BATTERY_OKAY))
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted -> if (!isGranted) {
                    Toast.makeText(this, "The app was not allowed to read your contacts", Toast.LENGTH_LONG).show()
                exitProcess(0)
                }
            }.launch(android.Manifest.permission.READ_CONTACTS)
        }
        val recycler = findViewById<RecyclerView>(R.id.contacts)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ListAdapter()
        adapter.addItem(ListAdapter.Item("test",0))
        recycler.adapter = adapter
        recycler.setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_DOWN) {
                Log.i(APP_NAME, "Start timer")
                mCountDownTimer.start()
            } else if (event?.action == MotionEvent.ACTION_DOWN) {
                Log.i(APP_NAME, "Stop timer")
                mCountDownTimer.cancel()
            }
            true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                Log.i(APP_NAME, "Volume Up key is pressed")
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                Log.i(APP_NAME, "Volume Down key is pressed")
            }
            KeyEvent.KEYCODE_BACK -> {
                Log.i(APP_NAME, "Back key is pressed")
            }
            KeyEvent.KEYCODE_RECENT_APPS -> {
                Log.i(APP_NAME, "Recent Apps key is pressed")
            }
            KeyEvent.KEYCODE_HOME -> {
                Log.i(APP_NAME, "Home key is pressed")
            }
            KeyEvent.KEYCODE_APP_SWITCH -> {
                Log.i(APP_NAME, "App Switch key is pressed")
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isLockTaskPermitted(packageName)) {
            startLockTask()
        } else {
            Log.i(APP_NAME, "Working in normal mode")
        }

    }
}