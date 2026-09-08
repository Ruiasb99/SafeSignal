package com.example.emergencybutton.ui.screens

import com.example.emergencybutton.ui.theme.SafeColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencybutton.ProtectionMode
import com.example.emergencybutton.SavedLocation
import com.example.emergencybutton.domain.locationAge

@Composable
fun EmergencyScreen(
    contacts: List<String>,
    mode: ProtectionMode,
    savedLocation: SavedLocation?,
    incidentActive: Boolean,
    hasCancellationPin: Boolean,
    cancellationPin: String,
    status: String,
    onModeChange: (ProtectionMode) -> Unit,
    onEditContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenButton: () -> Unit,
    buttonStatus: String,
    onCancellationPinChange: (String) -> Unit,
    onCancelEmergency: () -> Unit,
    onEmergencyClick: () -> Unit
) {
    val pageBackground = Brush.verticalGradient(
        listOf(SafeColors.Background, Color(0xFFF9F5FF), SafeColors.WarmBackground)
    )
    val modeColor = when (mode) {
        ProtectionMode.ARMED -> Color(0xFF138A62)
        ProtectionMode.LOW_POWER -> Color(0xFFB26A00)
        ProtectionMode.OFF -> Color(0xFF68747A)
    }
    val modeTitle = when (mode) {
        ProtectionMode.ARMED -> "Location cache: armed"
        ProtectionMode.LOW_POWER -> "Location cache: low power"
        ProtectionMode.OFF -> "Location caching off"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(pageBackground)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SafeColors.Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Column {
                Text(
                    "SafeSignal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SafeColors.Ink
                )
                Text(
                    "Your personal SOS companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SafeColors.Muted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SafeColors.Panel),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(modeColor, CircleShape)
                    )
                    Text(
                        modeTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (savedLocation == null) {
                        "No location has been saved yet"
                    } else {
                        "Saved location: " +
                            locationAge(savedLocation.timestampMillis)
                    },
                    color = Color(0xFFC5DADF),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Save a recent location",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF243A41)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProtectionMode.entries.forEach { option ->
                val selected = option == mode
                val label = when (option) {
                    ProtectionMode.ARMED -> "Armed"
                    ProtectionMode.LOW_POWER -> "Low power"
                    ProtectionMode.OFF -> "Off"
                }
                val selectedColor = when (option) {
                    ProtectionMode.ARMED -> Color(0xFF138A62)
                    ProtectionMode.LOW_POWER -> Color(0xFFB26A00)
                    ProtectionMode.OFF -> Color(0xFF68747A)
                }
                if (selected) {
                    Button(
                        onClick = { onModeChange(option) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedColor
                        )
                    ) {
                        Text(
                            label,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = { onModeChange(option) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = when (option) {
                                ProtectionMode.ARMED -> SafeColors.Mint
                                ProtectionMode.LOW_POWER -> SafeColors.Amber
                                ProtectionMode.OFF -> Color(0xFFE5E8ED)
                            },
                            contentColor = when (option) {
                                ProtectionMode.ARMED -> SafeColors.Forest
                                ProtectionMode.LOW_POWER -> SafeColors.Bronze
                                ProtectionMode.OFF -> SafeColors.Muted
                            }
                        )
                    ) {
                        Text(
                            label,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SafeColors.Mint),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Emergency contacts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF243A41)
                    )
                    Text(
                        contacts.size.toString() +
                            if (contacts.size == 1) " contact will be alerted"
                            else " contacts will be alerted",
                        style = MaterialTheme.typography.bodySmall,
                        color = SafeColors.Muted
                    )
                }
                FilledTonalButton(
                    onClick = onEditContacts,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = SafeColors.Forest, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Edit contacts", maxLines = 1)
                }
            }
        }

        if (incidentActive) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFE8E6)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Emergency alert active",
                        color = Color(0xFF9F1C1C),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Enter your cancellation PIN to notify every contact that you are safe.",
                        color = Color(0xFF73413D),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (hasCancellationPin) {
                        OutlinedTextField(
                            value = cancellationPin,
                            onValueChange = onCancellationPinChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Cancellation PIN") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    Button(
                        onClick = if (hasCancellationPin) {
                            onCancelEmergency
                        } else {
                            onOpenSettings
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9F1C1C)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (hasCancellationPin) {
                                "Cancel emergency"
                            } else {
                                "Create cancellation PIN"
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (status != "Ready") Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFD8E2E5), RoundedCornerShape(14.dp)),
            color = Color(0xF5FFFFFF),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = status,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                textAlign = TextAlign.Center,
                color = SafeColors.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (contacts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onEmergencyClick,
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
                shape = CircleShape,
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD52B2B),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 3.dp
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SOS",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "TAP FOR HELP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Sends an emergency alert to every saved contact",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = SafeColors.Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onOpenButton,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SafeColors.Amber, contentColor = SafeColors.Bronze),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Emergency button · Bluetooth", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(buttonStatus, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onOpenSettings,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SafeColors.Lavender, contentColor = SafeColors.Plum),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "⚙",
                fontSize = 22.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                "Settings",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
