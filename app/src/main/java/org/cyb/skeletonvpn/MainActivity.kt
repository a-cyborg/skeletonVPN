package org.cyb.skeletonvpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import org.cyb.skeletonvpn.util.Prefs
import org.cyb.skeletonvpn.util.isAcceptableIpAddress
import org.cyb.skeletonvpn.util.isAcceptablePortNumber


class MainActivity : AppCompatActivity() {
    private val TAG = this@MainActivity::class.java.simpleName

    private lateinit var dashboard: TextView
    private lateinit var serverAddrView: EditText
    private lateinit var serverPortView: EditText
    private lateinit var sharedSecretView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dashboard = findViewById(R.id.dashboard_textview)
        serverAddrView = findViewById(R.id.server_addr)
        serverPortView = findViewById(R.id.server_port)
        sharedSecretView = findViewById(R.id.shared_secret)
    }

    fun startVpnService(view: View) {
        val serverAddr = serverAddrView.text.toString()
        val port = serverPortView.text.toString()
        val sharedSecret = sharedSecretView.text.toString()

        if (isAcceptableIpAddress(serverAddr) and isAcceptablePortNumber(port)) {
            saveInputsToShredPrefs(serverAddr, port, sharedSecret)
            prepareVpnService()
        } else {
            dashboard.setText(R.string.unacceptable_inputs)
        }
    }

    private fun saveInputsToShredPrefs(serverAddr: String, port: String, secret: String) {
        with (getSharedPreferences(Prefs.NAME.key, MODE_PRIVATE).edit()) {
            putString(Prefs.SERVER_ADDRESS.key, serverAddr)
            putString(Prefs.SERVER_PORT.key, port)
            putString(Prefs.SHARED_SECRET.key, secret)
            commit()
        }
    }

    private fun prepareVpnService() {
        // Prepare the app to become the user's current VPN service.
        // If user hasn't given permission `VpnService.prepare()` returns an activity intent.
        val intent = VpnService.prepare(this)

        if (intent == null) {
            startService(getServiceIntentWithAction())
        } else {
            permissionActivityLauncherForResult.launch(intent)
        }
    }

    private val permissionActivityLauncherForResult =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                startService(getServiceIntentWithAction())
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
    }

    fun stopVpnService(view: View) {
        startService(
            getServiceIntentWithAction(SkeletonVpnService.DISCONNECT_ACTION)
        )
    }

    private fun getServiceIntentWithAction(action: String = SkeletonVpnService.CONNECT_ACTION)
    : Intent {
        return Intent(this, SkeletonVpnService::class.java)
            .setAction(action)
    }
}