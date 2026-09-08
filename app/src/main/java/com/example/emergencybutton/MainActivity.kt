package com.example.emergencybutton

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import com.example.emergencybutton.platform.ButtonMonitoringService
import android.provider.Settings
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.example.emergencybutton.platform.AndroidContactNumberReader
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.emergencybutton.ui.EmergencyApp
import com.example.emergencybutton.ui.EmergencyViewModel
import com.example.emergencybutton.ui.PermissionAction
import com.example.emergencybutton.ui.account.AccountViewModel
import com.example.emergencybutton.ui.contacts.AppContactsViewModel

/** Android entry point: hosts Compose and handles platform permission dialogs. */
class MainActivity : ComponentActivity() {
    private var pendingButtonStart: Boolean? = null
    private val container get() = (application as EmergencyButtonApplication).container
    private val viewModel: EmergencyViewModel by lazy {
        ViewModelProvider(this, container)[EmergencyViewModel::class.java]
    }
    private val accountViewModel: AccountViewModel by lazy {
        ViewModelProvider(this, container)[AccountViewModel::class.java]
    }
    private val appContactsViewModel: AppContactsViewModel by lazy {
        ViewModelProvider(this, container)[AppContactsViewModel::class.java]
    }

    private val emergencyPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.onEmergencyPermissionResult(hasEmergencyPermissions())
        }

    private val modePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            handlePermissionAction(viewModel.onModePermissionResult(
                hasForegroundLocationPermission(), hasBackgroundLocationPermission()
            ))
        }

    private val buttonPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val start = pendingButtonStart ?: return@registerForActivityResult
            pendingButtonStart = null
            if (hasPermission(Manifest.permission.BLUETOOTH_SCAN) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                if (start) startButtonMonitoring() else container.ble.discover()
            } else container.ble.message("Nearby devices permission is required. You can allow it in Android app settings")
        }

    private val contactPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        if (uri.scheme != "content") return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val number = withContext(Dispatchers.IO) { AndroidContactNumberReader(this@MainActivity).read(uri) }
                if (number.isNullOrBlank()) contactPickerError() else viewModel.contactPicked(number)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { contactPickerError() }
        }
    }

    private fun pickContact() {
        try {
            contactPicker.launch(Intent(Intent.ACTION_PICK).apply { type = Phone.CONTENT_TYPE })
        } catch (_: ActivityNotFoundException) { contactPickerError() }
    }

    private fun contactPickerError() {
        Toast.makeText(this, "Could not read that phone number. You can enter it manually instead.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(0xFFF4F6FC.toInt(), 0xFFF4F6FC.toInt()))
        setContent {
            val buttonState by container.ble.states.collectAsState()
            EmergencyApp(
                viewModel = viewModel,
                accountViewModel = accountViewModel,
                appContactsViewModel = appContactsViewModel,
                onSelectMode = { mode ->
                    handlePermissionAction(viewModel.selectMode(
                        mode, hasForegroundLocationPermission(), hasBackgroundLocationPermission()
                    ))
                },
                onBeginEmergency = {
                    handlePermissionAction(viewModel.beginEmergency(hasEmergencyPermissions()))
                },
                buttonState = buttonState,
                onScanButton = { requestButtonPermissions(false) },
                onStartButton = { requestButtonPermissions(true) },
                onStopButton = { stopService(Intent(this, ButtonMonitoringService::class.java)) },
                onSelectButton = container.ble::remember,
                onForgetButton = container.ble::forget,
                onPickContact = ::pickContact,
                onStopButtonScan = container.ble::stopDiscovery
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume(hasBackgroundLocationPermission())
    }

    private fun handlePermissionAction(action: PermissionAction) {
        when (action) {
            PermissionAction.NONE -> Unit
            PermissionAction.EMERGENCY -> emergencyPermissionLauncher.launch(
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            PermissionAction.MODE_LOCATION -> modePermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            PermissionAction.BACKGROUND_SETTINGS -> startActivity(Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")
            ))
        }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasForegroundLocationPermission() =
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun hasBackgroundLocationPermission() =
        hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun hasEmergencyPermissions() =
        hasPermission(Manifest.permission.SEND_SMS) && hasForegroundLocationPermission()

    private fun requestButtonPermissions(start: Boolean) {
        val permissions = mutableListOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (start) {
            permissions.addAll(listOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
            if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        pendingButtonStart = start
        buttonPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startButtonMonitoring() {
        val issue = ButtonMonitoringService.setupIssue(this)
        when {
            issue != null -> container.ble.message(issue)
            container.emergencyRepository.loadContacts().isEmpty() -> container.ble.message("Save an SMS contact first")
            !container.pins.hasPin() -> container.ble.message("Create a cancellation PIN in Settings first")
            else -> try {
                ContextCompat.startForegroundService(this, Intent(this, ButtonMonitoringService::class.java))
            } catch (_: RuntimeException) {
                container.ble.message("Android prevented monitoring. Keep the app open and try again")
            }
        }
    }
}
