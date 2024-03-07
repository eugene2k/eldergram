package com.example.eldergram

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class KeypadActivity : AppCompatActivity() {
    private val mLog = Logger(this)
    var tgService: EldergramService? = null
    protected abstract val mErrorText: Int
    protected abstract val mSubmitText: Int
    protected abstract val mPromptText: Int
    protected abstract val mInitialText: String
    protected abstract val mSubmitHandler: (EldergramService, String, (Any) -> Unit) -> Unit
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            tgService = (service as EldergramService.ServiceBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            tgService = null
        }
    }

    fun setError() {
        val errText = findViewById<TextView>(R.id.error_text_prompt)
        if (errText.background == null) {
            mLog.e("background is null")
        }
        errText.background.setTint(Color.RED)
        errText.setText(mErrorText)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.numpad)
        val prompt = findViewById<TextView>(R.id.text_prompt)
        val numberField = findViewById<EditText>(R.id.editTextNumber)
        val submitButton = findViewById<Button>(R.id.button_submit)
        prompt.setText(mPromptText)
        numberField.setText(mInitialText)
        submitButton.setText(mSubmitText)

        bindService(
            Intent(this@KeypadActivity, EldergramService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun enterNumber(view: View) {
        val numberField = findViewById<EditText>(R.id.editTextNumber)
        numberField.text.append((view as Button).text)
    }

    fun submit(view: View) {
        val errorPrompt = findViewById<TextView>(R.id.error_text_prompt)
        errorPrompt.setText("")
        errorPrompt.background.setTint(Color.WHITE)
        val numberField = findViewById<EditText>(R.id.editTextNumber)
        tgService?.let {
            val handler = Handler(Looper.getMainLooper())
            mSubmitHandler(it, numberField.text.toString()) {
                handler.post(Runnable { setError() })
            }
        }
    }

    fun clear(view: View) {
        val numberField = findViewById<EditText>(R.id.editTextNumber)
        numberField.setText(mInitialText)
    }

    fun delete(view: View) {
        val numberField = findViewById<EditText>(R.id.editTextNumber)
        val len = numberField.text.length
        val offset = mInitialText.length

        if (len > offset) {
            numberField.text.delete(len - 1, len)
        }
    }
}

class EnterPhoneNumberActivity : KeypadActivity() {
    override val mInitialText = "+"
    override val mErrorText = R.string.error_invalid_phone_number
    override val mSubmitText = R.string.submit_phone
    override val mPromptText = R.string.phone_prompt
    override val mSubmitHandler = EldergramService::sendAuthPhone
}

class EnterAuthCodeActivity : KeypadActivity() {
    override val mInitialText = ""
    override val mErrorText = R.string.error_invalid_code
    override val mSubmitText = R.string.submit_code
    override val mPromptText = R.string.auth_code_prompt
    override val mSubmitHandler = EldergramService::sendAuthCode
}