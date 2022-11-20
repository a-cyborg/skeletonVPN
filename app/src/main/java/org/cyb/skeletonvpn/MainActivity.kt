package org.cyb.skeletonvpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.*


class MainActivity : AppCompatActivity() {
    private val TAG = this@MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startVpnService(view: View) {
        // Prepare the app to become the user's current VPN service.
        // If user hasn't given permission `VpnService.prepare()` returns an activity intent.
        VpnService.prepare(this)
            ?.let { intent -> permissionActivityLauncherForResult.launch(intent) }
            ?: run { startService(getActionSettedVpnServiceIntent()) } // null
    }

    private val permissionActivityLauncherForResult =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                startService(getActionSettedVpnServiceIntent())
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
    }

    fun stopVpnService(view: View) {
        startService(
            getActionSettedVpnServiceIntent(SkeletonVpnService.DISCONNECT_ACTION)
        )
    }

    private fun getActionSettedVpnServiceIntent(action: String = SkeletonVpnService.CONNECT_ACTION)
    : Intent {
        return Intent(this, SkeletonVpnService::class.java)
            .setAction(action)
    }
}