package com.example.eldergram

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import android.util.Log
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.ComponentName
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        val prefMan = PreferenceManager.getDefaultSharedPreferences(this)

        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val darComponentName = ComponentName(this, MainActivity.MyDeviceAdminReceiver().javaClass)

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, darComponentName)
        val getAdmin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Log.i(APP_NAME, "Lock task setting enabled")
            } else {
                Log.i(APP_NAME, "Failed to acquire device owner permissions")
            }
        }
        prefMan
            .edit()
            .putBoolean("adminMode", dpm.isAdminActive(darComponentName))
            .putBoolean("lockTaskMode", dpm.isLockTaskPermitted(packageName))
            .apply()

        prefMan.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            val prefs = sharedPreferences.all
            when (key) {
                "adminMode" -> {
                    if (prefs["adminMode"] as Boolean) {
                        getAdmin.launch(intent)
                    } else {
                        dpm.removeActiveAdmin(darComponentName)
                    }
                }
                "lockTaskMode" -> {
                    if (prefs["lockTaskMode"] as Boolean) {
                        dpm.setLockTaskPackages(darComponentName, arrayOf(packageName))
                    } else {
                        dpm.setLockTaskPackages(darComponentName, arrayOf())
                    }
                }
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}