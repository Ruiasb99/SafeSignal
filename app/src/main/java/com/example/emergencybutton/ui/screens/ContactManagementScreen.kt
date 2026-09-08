package com.example.emergencybutton.ui.screens

import com.example.emergencybutton.ui.theme.SafeColors
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContactManagementScreen(
    contacts: List<String>,
    contactDraft: String,
    editingContact: String?,
    status: String,
    onContactDraftChange: (String) -> Unit,
    onPickContact: () -> Unit,
    onSaveContact: () -> Unit,
    onEditContact: (String) -> Unit,
    onRemoveContact: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onAppContacts: () -> Unit,
    onBack: () -> Unit
) {
    var removing by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    removing?.let { number ->
        AlertDialog(onDismissRequest = { removing = null },
            title = { Text("Remove SMS contact?") },
            text = { Text("This number will no longer receive new SOS alerts. An active alert keeps its original recipients.") },
            confirmButton = { TextButton(onClick = { onRemoveContact(number); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Keep contact") } })
    }

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
            "SMS contacts",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SafeColors.Ink
        )

        Spacer(modifier = Modifier.height(18.dp))
        FilledTonalButton(onClick = onAppContacts, modifier = Modifier.fillMaxWidth()) {
            Text("Manage app contacts")
        }
        Text(
            if (contacts.isEmpty()) {
                "Add at least one contact before returning to the dashboard."
            } else {
                "Every saved contact receives your emergency alerts."
            },
            color = SafeColors.Muted
        )
        Spacer(modifier = Modifier.height(12.dp))

        contacts.forEachIndexed { index, number ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 14.dp,
                        end = 6.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(SafeColors.Mint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (index + 1).toString(),
                            color = SafeColors.Forest,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        number,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        color = Color(0xFF243A41),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    TextButton(onClick = { onEditContact(number) }) {
                        Text("Edit", fontSize = 12.sp)
                    }
                    TextButton(onClick = { removing = number }) {
                        Text("Remove", color = Color(0xFFB3261E), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (editingContact == null) "Add a contact" else "Update contact",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF243A41)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onPickContact, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose from phone contacts")
                }
                Text("Or enter a number below. Check its country code before saving.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = contactDraft,
                    onValueChange = onContactDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Phone number") },
                    placeholder = { Text("+49 123 456789") },
                    supportingText = { Text("Include country code, e.g. +351 or +49") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSaveContact,
                    enabled = contactDraft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeColors.Primary
                    )
                ) {
                    Text(if (editingContact == null) "Save contact" else "Update contact")
                }
                if (editingContact != null) {
                    TextButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cancel edit")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (status != "Ready") Text(
            status,
            modifier = Modifier.fillMaxWidth(),
            color = SafeColors.Muted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}
