package org.cyb.skeletonvpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : AppCompatActivity() {
    private val TAG = this@MainActivity::class.java.simpleName

    private val permissionActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startService(getService().setAction(SkeletonVpnService.ACTION_CONNECT))
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startVPN(view: View) {
        // Prepare the app to become the user's current VPN service.
        val permissionIntent = VpnService.prepare(this)

        if (permissionIntent != null) {
            permissionActivityLauncher.launch(permissionIntent)
        } else {
            startService(getService().setAction(SkeletonVpnService.ACTION_CONNECT))
        }
    }

    fun stopVPN(view: View) {
        startService(getService().setAction(SkeletonVpnService.ACTION_DISCONNECT))
    }

    private fun getService() = Intent(this, SkeletonVpnService::class.java)
}