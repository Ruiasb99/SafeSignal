package com.example.emergencybutton.ui.screens

import com.example.emergencybutton.ui.theme.SafeColors
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.emergencybutton.domain.BleState

@Composable
fun ButtonScreen(state: BleState, onScan: () -> Unit, onStart: () -> Unit,
                 onStop: () -> Unit, onSelect: (String) -> Unit, onForget: () -> Unit,
                 onStopScan: () -> Unit, onBack: () -> Unit) {
    var confirmStart by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    val stopScan by rememberUpdatedState(onStopScan)
    DisposableEffect(Unit) { onDispose { stopScan() } }
    Column(Modifier.fillMaxSize().background(SafeColors.Background)
        .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your emergency button", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = SafeColors.Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.status, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Press 3 or more times within 2 seconds. SOS starts on press 3; extra presses won't resend while an emergency is active.",
                    color = Color(0xFFC5DADF))
            }
        }
        Text("Monitoring is separate from the location-cache modes. No alarm sound, flash or call is added. Android still shows a monitoring notification.",
            style = MaterialTheme.typography.bodySmall)
        state.savedAddress?.let { Text("Selected button: $it", style = MaterialTheme.typography.bodyMedium) }
        if (!state.monitoring) {
            OutlinedButton(onClick = onScan, enabled = !state.scanning, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.scanning) "Scanning…" else "Scan for button")
            }
            state.devices.forEach { device ->
                OutlinedButton(onClick = { onSelect(device.address) }, modifier = Modifier.fillMaxWidth()) {
                    Column { Text(device.name); Text(device.address, style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (state.savedAddress != null) {
                Button(onClick = { confirmStart = true }, modifier = Modifier.fillMaxWidth()) { Text("Start monitoring") }
                TextButton(onClick = onForget) { Text("Forget this button") }
            }
        } else {
            Text("Press events received this session: ${state.receivedPresses}")
            state.characteristic?.let { Text("Notification characteristic: $it", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = { confirmStop = true }, modifier = Modifier.fillMaxWidth()) { Text("Stop monitoring") }
        }
        Text("Prototype: test with a consenting contact. After reboot, force-stop or Android stopping the service, reopen this page and start monitoring again. BLE is not yet securely bonded.",
            style = MaterialTheme.typography.bodySmall)
    }
    if (confirmStart) AlertDialog(onDismissRequest = { confirmStart = false },
        title = { Text("Enable real SOS messages?") },
        text = { Text("Three quick button presses will send real SMS alerts and location to every saved SMS contact, even with the screen locked. Use the updated Arduino sketch and tell your test contacts first.") },
        confirmButton = { TextButton(onClick = { confirmStart = false; onStart() }) { Text("Enable monitoring") } },
        dismissButton = { TextButton(onClick = { confirmStart = false }) { Text("Not yet") } })
    if (confirmStop) AlertDialog(onDismissRequest = { confirmStop = false },
        title = { Text("Disconnect your button?") },
        text = { Text("The physical button will no longer send SOS. This does not cancel an active emergency; use your PIN on the dashboard for that.") },
        confirmButton = { TextButton(onClick = { confirmStop = false; onStop() }) { Text("Stop monitoring") } },
        dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Keep monitoring") } })
}
