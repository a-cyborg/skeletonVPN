package org.cyb.skeletonvpn.ui

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import org.cyb.skeletonvpn.databinding.ActivityMainBinding
import org.cyb.skeletonvpn.utils.ServerInfoRepository
import org.cyb.skeletonvpn.utils.getBaseMessageOfState
import org.cyb.skeletonvpn.utils.getColorOfState
import org.cyb.skeletonvpn.vpn.VpnServiceStarter

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory(
            ServerInfoRepository.getInstance(this.applicationContext),
            VpnServiceStarter.getInstance(this.applicationContext)
        )
    }

    private val permissionActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.handlePermissionActivityResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.lifecycleOwner = this
        binding.mainViewModel = viewModel

        viewModel.observePermissionIntent().observe(this) {
            it?.let {
                permissionActivityLauncher.launch(it)
            }
        }

        viewModel.observeVpnState().observe(this) {
            binding.colorStateView.setBackgroundResource(getColorOfState(it.connectionState))
            binding.colorStateView.text = getString(getBaseMessageOfState(it.connectionState))
            binding.dashboardView.text = it.stateMsg
        }
    }
}