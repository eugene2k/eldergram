package com.example.eldergram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi

class EldergramService : Service() {
    companion object {
        const val UPDATE_INTENT = "AUTH_STATE_NOTIFICATION"
    }

    enum class ClientState {
        AuthWaitPhone,
        AuthWaitCode,
        Ready;
    }

    class Contact(
        val firstName: String,
        val lastName: String,
        var phoneNumbers: Array<Pair<String, Long>>,
    )

    class UserInfo(
        val canBeCalled: Boolean,
    )

    class ServiceBinder(val service: EldergramService) : Binder()

    class TdClient(updateHandler: Client.ResultHandler) {
        private val mClient: Client = Client.create(updateHandler, null, null)

        init {
            Log.i(APP_NAME, "Client initialized")
        }

        fun send(query: TdApi.Function, resultHandler: (TdApi.Function, TdApi.Object) -> Unit) {
            mClient.send(query) {
                resultHandler(query, it)
            }
        }

        fun send(query: TdApi.Function, resultHandler: Client.ResultHandler) {
            mClient.send(query, resultHandler)
        }
    }

    private val mLog = Logger(this)

    private val updateHandler: Client.ResultHandler = Client.ResultHandler {
        mLog.i("Received update message: %s".format(it))
        when (it.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val state = (it as TdApi.UpdateAuthorizationState).authorizationState

                when (state.constructor) {
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
                        mClient.send(TdApi.SetTdlibParameters(params), ::defaultResultHandler)
                    }

                    TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                        mClient.send(TdApi.CheckDatabaseEncryptionKey(), ::defaultResultHandler)
                    }

                    TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                        val phoneNumber = prefs.getString("phone", "").orEmpty()

                        if (phoneNumber.isEmpty()) {
                            broadcastAuthStateNotification(
                                ClientState.AuthWaitPhone
                            )
                        } else {
                            mClient.send(
                                TdApi.SetAuthenticationPhoneNumber(phoneNumber, null),
                                Client.ResultHandler {
                                    if (it.constructor == TdApi.Error.CONSTRUCTOR) {
                                        broadcastAuthStateNotification(
                                            ClientState.AuthWaitPhone
                                        )
                                    }
                                })
                        }
                    }

                    TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                        broadcastAuthStateNotification(ClientState.AuthWaitCode)
                    }

                    TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                        broadcastAuthStateNotification(ClientState.Ready)
                    }

                    else -> {
                        mLog.e("Unhandled authorization state!")
                    }
                }
            }

            TdApi.UpdateCall.CONSTRUCTOR -> {
                mLog.i((it as TdApi.UpdateCall).call.toString())
            }
        }
    }
    private var mClient: TdClient = TdClient(updateHandler)

    private fun defaultResultHandler(query: TdApi.Function, result: TdApi.Object) {
        when (result.constructor) {
            TdApi.Error.CONSTRUCTOR -> {
                val err = (result as TdApi.Error)
                mLog.e(
                    "Error(%d) calling %s: %s".format(
                        err.code,
                        query,
                        err.message
                    )
                )
            }

            TdApi.Ok.CONSTRUCTOR -> {
                mLog.i(
                    "Calling %s Ok!".format(query)
                )
            }

            else -> {
                mLog.e(
                    "Unrecognised result: %s".format(result.javaClass.toString())
                )
            }
        }

    }

    private fun broadcastAuthStateNotification(state: ClientState) {
        sendBroadcast(
            Intent(UPDATE_INTENT).putExtra(
                "STATE",
                state.ordinal
            ).setPackage(packageName)
        )
    }

    fun callTdFunctionWithResult(f: TdApi.Function, callBack: (TdApi.Object) -> Unit) {
        mClient.send(f, Client.ResultHandler {
            callBack(it)
        })
    }

    fun sendAuthPhone(phoneNumber: String, callBack: (Int) -> Unit) {
        callTdFunctionWithResult(
            TdApi.SetAuthenticationPhoneNumber(phoneNumber, null)
        ) {
            callBack((it as TdApi.Error).code)
        }
    }

    fun sendAuthCode(authCode: String, callBack: (Int) -> Unit) {
        callTdFunctionWithResult(TdApi.CheckAuthenticationCode(authCode)) {
            callBack((it as TdApi.Error).code)
        }
    }

    fun importContacts(
        contacts: Array<Contact>,
        callBack: (Array<Array<Int>>) -> Unit
    ) {
        val c = contacts.flatMap {
            it.phoneNumbers.map { pair ->
                TdApi.Contact(
                    pair.first,
                    it.firstName,
                    it.lastName,
                    "",
                    0
                )
            }
        }
        callTdFunctionWithResult(TdApi.ImportContacts(c.toTypedArray())) {
            val contactIds = ArrayList<Array<Int>>()
            val userIds = (it as TdApi.ImportedContacts).userIds
            var userIdIdx = 0
            for (contactIdx in 0..contacts.size) {
                val contact = contacts[contactIdx]
                val arrayList = ArrayList<Int>()
                for (phoneIdx in 0..contact.phoneNumbers.size) {
                    arrayList.add(userIds[userIdIdx].toInt())
                    userIdIdx++
                }
                contactIds.add(arrayList.toTypedArray())
            }
            callBack(contactIds.toTypedArray())

        }
    }

    fun getUserInfo(uid: Int, callBack: (UserInfo) -> Unit) {
        callTdFunctionWithResult(TdApi.GetUserFullInfo(uid.toLong())) {
            val info = (it as TdApi.UserFullInfo)
            callBack(UserInfo(info.canBeCalled))
        }
    }

    override fun onCreate() {
        super.onCreate()
        mLog.i("Service initialized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel("default", "def", NotificationManager.IMPORTANCE_LOW)
                )
                "default"
            } else {
                NotificationChannelCompat.DEFAULT_CHANNEL_ID
            }
        val notification =
            NotificationCompat.Builder(this, notificationId)
                .build()
        startForeground(1, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mLog.i("Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return ServiceBinder(this)
    }
}
