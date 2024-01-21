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
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import kotlin.system.exitProcess

const val APP_NAME = "eldergram"

class MainActivity : ComponentActivity() {
    class MyDeviceAdminReceiver : DeviceAdminReceiver()

    class TdClient(updateHandler: Client.ResultHandler) {
        class ResultHandler(private val query: TdApi.Function) : Client.ResultHandler {
            override fun onResult(it: TdApi.Object) {
                when (it.constructor) {
                    TdApi.Error.CONSTRUCTOR -> {
                        val err = (it as TdApi.Error)
                        Log.e(
                            APP_NAME,
                            "Error(%d) calling %s: %s".format(
                                err.code,
                                query,
                                err.message
                            )
                        )
                    }

                    TdApi.Ok.CONSTRUCTOR -> {
                        Log.i(
                            APP_NAME,
                            "Calling %s Ok!".format(query)
                        )
                    }

                    else -> {
                        Log.e(APP_NAME, "Unrecognised result: %s".format(it.javaClass.toString()))
                    }
                }
            }
        }

        private val mClient: Client = Client.create(updateHandler, null, null)
        fun send(query: TdApi.Function) {
            mClient.send(query, ResultHandler(query))
        }

        fun send(query: TdApi.Function, resultHandler: Client.ResultHandler) {
            mClient.send(query, resultHandler)
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
    private val authCode = ""
    private val updateHandler: Client.ResultHandler = Client.ResultHandler {
        Log.i(APP_NAME, "Received update message: %s".format(it))
        when (it.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val authState = (it as TdApi.UpdateAuthorizationState).authorizationState

                when (authState.constructor) {
                    TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                        val params = TdApi.TdlibParameters()
                        params.apiHash = "5d0453b7ad9d9e6e510e942f1cb41441"
                        params.apiId = 21061468
                        params.deviceModel = "Phone"
                        params.applicationVersion = "1.0"
                        params.useFileDatabase = false
                        params.useChatInfoDatabase = false
                        params.useMessageDatabase = false
                        params.useSecretChats = false
                        params.databaseDirectory = applicationContext.dataDir.absolutePath
                        params.enableStorageOptimizer = true
                        params.systemLanguageCode = "en"
                        params.useTestDc = true
                        mClient.send(TdApi.SetTdlibParameters(params))
                    }

                    TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                        mClient.send(TdApi.CheckDatabaseEncryptionKey())
                    }

                    TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                        val phoneNumber = "+37281618658"
                        mClient.send(
                            TdApi.SetAuthenticationPhoneNumber(phoneNumber, null)
                        )
                    }

                    TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                        val codeInfo = (authState as TdApi.AuthorizationStateWaitCode).codeInfo
//                        mClient.send(TdApi.ResendAuthenticationCode())
//                        when (codeInfo.type.constructor) {
//
//                        }
//                        mClient.send(TdApi.CheckAuthenticationCode(authCode))
//                        {
//                            when (it.constructor) {
//                                TdApi.AuthenticationCodeTypeTelegramMessage.CONSTRUCTOR -> {
//
//                                }
//                            }
//                        }
                    }

                    else -> {
                        Log.e(APP_NAME, "Unhandled authorization state!")
//                        exitProcess(0)
                    }
                }
            }
        }
    }
    private val mClient = TdClient(updateHandler)

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
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(
                        this,
                        "The app was not allowed to read your contacts",
                        Toast.LENGTH_LONG
                    ).show()
                    exitProcess(0)
                }
            }.launch(android.Manifest.permission.READ_CONTACTS)
        }
        val recycler = findViewById<RecyclerView>(R.id.contacts)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ListAdapter()

        adapter.addItem(ListAdapter.Item("test", 0))
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
}