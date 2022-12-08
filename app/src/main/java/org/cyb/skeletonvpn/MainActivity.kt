package org.cyb.skeletonvpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import org.cyb.skeletonvpn.util.*
import java.io.IOException
import java.util.InputMismatchException


class MainActivity : AppCompatActivity() {
    private val TAG = this@MainActivity::class.java.simpleName

    private lateinit var dashboard: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dashboard = findViewById(R.id.dashboard_textview)
    }

    fun connectButtonClicked(view: View) {
        try {
            collectUserInputAndSaveToSharedPreferences()
            startVpnService()
        } catch (error: InputMismatchException) {
            dashboard.text = error.message
        }
    }

    @Throws
    private fun collectUserInputAndSaveToSharedPreferences() {
        val serverAddress = findViewById<EditText>(R.id.server_addr).text.toString()
        val serverPort = findViewById<EditText>(R.id.server_port).text.toString()
        val sharedSecret = findViewById<EditText>(R.id.shared_secret).text.toString()

        ServerInfo(serverAddress, serverPort, sharedSecret)
            .ifIsValidAddressThenSaveToSharedPrefs(this)
    }

    private fun startVpnService() {
        // Prepare the app to become the user's current VPN service.
        // If user hasn't given permission `VpnService.prepare()` returns an activity intent.
        when (val intent = VpnService.prepare(this)) {
            null -> startService(getServiceIntentWithAction())
            else -> permissionActivityLauncherForResult.launch(intent)
        }
    }

    private val permissionActivityLauncherForResult =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                RESULT_OK -> startService(getServiceIntentWithAction())
                else -> dashboard.setText(R.string.permission_denied)
            }
    }

    fun stopVpnService(view: View) {
        startService(
            getServiceIntentWithAction(SkeletonVpnService.DISCONNECT_ACTION)
        )
    }

    private fun getServiceIntentWithAction(
        action: String = SkeletonVpnService.CONNECT_ACTION
    ): Intent {
        return Intent(this, SkeletonVpnService::class.java)
            .setAction(action)
    }
}