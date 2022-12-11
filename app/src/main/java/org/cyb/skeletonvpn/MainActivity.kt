package org.cyb.skeletonvpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
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

        setSavedServerInfoToEditText()
    }

    private fun setSavedServerInfoToEditText() {
        val serverInfo = getServerInfoFromSharedPreferences(this)

        serverAddrView.setText(serverInfo.serverAddr)
        serverPortView.setText(serverInfo.serverPort)
        sharedSecretView.setText(serverInfo.sharedSecret)
    }

    fun connectButtonClicked(view: View) {
        try {
            collectUserInputAndSaveToSharedPreferences()
            startVpnService()
        } catch (exception: InputMismatchException) {
            dashboard.text = exception.message
        }
    }

    @Throws
    private fun collectUserInputAndSaveToSharedPreferences() {
        val serverAddress = serverAddrView.text.toString()
        val serverPort = serverPortView.text.toString()
        val sharedSecret = sharedSecretView.text.toString()

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