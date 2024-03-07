package com.example.eldergram

import android.annotation.SuppressLint
import android.app.admin.DeviceAdminReceiver
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking


const val APP_NAME = "eldergram"

class Logger(private val ctx: Context) {
    fun i(s: String) {
        val tag = "%s::%s".format(ctx.packageName, ctx.javaClass.name)
        Log.i(tag, s)
    }

    fun e(s: String) {
        val tag = "%s::%s".format(ctx.packageName, ctx.javaClass.name)
        Log.e(tag, s)
    }
}

class MainActivity : AppCompatActivity() {
    class MyDeviceAdminReceiver : DeviceAdminReceiver()

    private var service: EldergramService? = null

    val mLog = Logger(this)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@MainActivity.service = (service as EldergramService.ServiceBinder).service

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            this@MainActivity.service = null
        }
    }
    private val mCountDownTimer = object : CountDownTimer(5000, 5500) {
        override fun onFinish() {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        override fun onTick(millisUntilFinished: Long) {
            TODO("Not yet implemented")
        }
    }
    private val batteryLowReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val tv = findViewById<TextView>(R.id.battery_info)
            tv.setTextColor(Color.RED)
        }
    }
    private val batteryChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val tv = findViewById<TextView>(R.id.battery_info)
            var batteryCharge = 0
            if (intent != null) {
                batteryCharge = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            }
            tv.text = getString(R.string.battery_info).format(batteryCharge)
        }
    }
    private val batteryOkayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val tv = findViewById<TextView>(R.id.battery_info)
            tv.setTextColor(Color.WHITE)
        }
    }

    private val updatesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mLog.i("Received ${EldergramService.UPDATE_INTENT}")
            intent?.getIntExtra("STATE", -1)?.let {
                when (it) {
                    EldergramService.ClientState.AuthWaitPhone.ordinal -> {
                        mLog.i("Starting phone number activity")
                        context?.startActivity(
                            Intent(
                                context,
                                EnterPhoneNumberActivity::class.java
                            )
                        )
                    }

                    EldergramService.ClientState.AuthWaitCode.ordinal -> {
                        mLog.i("Starting auth code activity")
                        context?.startActivity(
                            Intent(
                                context,
                                EnterAuthCodeActivity::class.java
                            )
                        )
                    }

                    EldergramService.ClientState.Ready.ordinal -> {
                        mLog.i("Starting main activity")
                        context?.startActivity(
                            Intent(
                                context,
                                MainActivity::class.java
                            )
                        )
                    }

                    else -> {
                        mLog.e("Error: Invalid ClientState")
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            updatesReceiver,
            IntentFilter(EldergramService.UPDATE_INTENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, EldergramService::class.java))
        } else {
            startService(Intent(this, EldergramService::class.java))
        }
        bindService(
            Intent(this, EldergramService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(
                        this,
                        "The app was not allowed to read your contacts",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    initialize()
                }
            }.launch(android.Manifest.permission.READ_CONTACTS)
        } else {
            initialize()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initialize() {
        setContentView(R.layout.contacts_list)
        registerReceiver(batteryChangedReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(batteryLowReceiver, IntentFilter(Intent.ACTION_BATTERY_LOW))
        registerReceiver(batteryOkayReceiver, IntentFilter(Intent.ACTION_BATTERY_OKAY))
        val recycler = findViewById<RecyclerView>(R.id.contacts)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ListAdapter()
        setContacts(adapter)
        recycler.adapter = adapter
        recycler.setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_DOWN) {
                mLog.i("Start timer")
                mCountDownTimer.start()
            } else if (event?.action == MotionEvent.ACTION_UP) {
                mLog.i("Stop timer")
                mCountDownTimer.cancel()
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updatesReceiver)
        unregisterReceiver(batteryChangedReceiver)
        unregisterReceiver(batteryLowReceiver)
        unregisterReceiver(batteryOkayReceiver)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                mLog.i("Volume Up key is pressed")
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                mLog.i("Volume Down key is pressed")
            }

            KeyEvent.KEYCODE_BACK -> {
                mLog.i("Back key is pressed")
            }

            KeyEvent.KEYCODE_RECENT_APPS -> {
                mLog.i("Recent Apps key is pressed")
            }

            KeyEvent.KEYCODE_HOME -> {
                mLog.i("Home key is pressed")
            }

            KeyEvent.KEYCODE_APP_SWITCH -> {
                mLog.i("App Switch key is pressed")
            }

            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    private fun setContacts(adapter: ListAdapter) {
        val contacts = arrayListOf<EldergramService.Contact>()
        val displayNames = arrayListOf<String>()
        val contactsCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.Data.DISPLAY_NAME
            ), null, null, null
        )
        contactsCursor?.let {
            while (contactsCursor.moveToNext()) {
                val contactId = contactsCursor.getInt(0)
                val firstName = contactsCursor.getString(1)
                val lastName = contactsCursor.getString(2)
                val displayName = contactsCursor.getString(3)
                displayNames.add(displayName)
                val phoneNumbersCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ), "${ContactsContract.Data.CONTACT_ID}=?", arrayOf(contactId.toString()), null
                )
                val numbers = ArrayList<Pair<String, Long>>()
                phoneNumbersCursor?.let { cursor ->
                    while (phoneNumbersCursor.moveToNext()) {
                        numbers.add(Pair(phoneNumbersCursor.getString(0), 0))
                    }
                    cursor.close()
                }
                contacts.add(EldergramService.Contact(firstName, lastName, numbers.toTypedArray()))
            }
            it.close()
        }

        val handler = Handler(Looper.getMainLooper())
        service?.importContacts(contacts.toTypedArray()) { contactIds ->
            handler.post {
                val pairs = displayNames.zip(contactIds).map {
                    Pair(it.first, it.second.filter { it != 0 })
                }.filter {
                    it.second.isNotEmpty()
                }
                pairs.forEach {
                    adapter.addItem(ListAdapter.Item(it.first, it.second[0]))
                }
            }
        }
    }
}