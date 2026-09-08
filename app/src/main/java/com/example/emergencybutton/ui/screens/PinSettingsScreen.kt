package com.example.emergencybutton.ui.screens

import com.example.emergencybutton.ui.theme.SafeColors
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PinSettingsScreen(
    hasExistingPin: Boolean,
    currentPin: String,
    newPin: String,
    confirmPin: String,
    status: String,
    onCurrentPinChange: (String) -> Unit,
    onNewPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onSave: () -> Unit,
    accountDescription: String,
    onOpenAccount: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    listOf(SafeColors.Background, SafeColors.Background)
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SafeColors.Ink
        )
        Text(
            "Protect emergency cancellation with a PIN only you know.",
            color = SafeColors.Muted
        )
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    if (hasExistingPin) "Change cancellation PIN"
                    else "Create cancellation PIN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF243A41)
                )
                Text(
                    "Use 4 to 6 digits. You will need this PIN to cancel an active alert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SafeColors.Muted
                )

                if (hasExistingPin) {
                    Spacer(modifier = Modifier.height(14.dp))
                    PinField(
                        value = currentPin,
                        onValueChange = onCurrentPinChange,
                        label = "Current PIN"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                PinField(
                    value = newPin,
                    onValueChange = onNewPinChange,
                    label = "New PIN"
                )
                Spacer(modifier = Modifier.height(12.dp))
                PinField(
                    value = confirmPin,
                    onValueChange = onConfirmPinChange,
                    label = "Confirm new PIN"
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                        newPin.length >= 4 &&
                            confirmPin.length >= 4 &&
                            (!hasExistingPin || currentPin.length >= 4),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeColors.Primary
                    )
                ) {
                    Text(if (hasExistingPin) "Update PIN" else "Create PIN")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SafeColors.Lavender)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("SafeSignal account", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = SafeColors.Ink)
                Text(accountDescription, color = SafeColors.Muted)
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = SafeColors.Plum, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)) {
                    Text("Manage account")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        if (status != "Ready") Text(
            status,
            modifier = Modifier.fillMaxWidth(),
            color = SafeColors.Muted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}
