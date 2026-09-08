package com.example.emergencybutton.ui.contacts

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.emergencybutton.domain.ContactInvitation
import com.example.emergencybutton.domain.InvitationStatus

@Suppress("DEPRECATION")
@Composable
fun AppContactsScreen(viewModel: AppContactsViewModel, onAccount: () -> Unit, onBack: () -> Unit) {
    val state = viewModel.state
    val clipboard = LocalClipboardManager.current
    var removing by remember { mutableStateOf<ContactInvitation?>(null) }
    state.createdInvitation?.let { invitation ->
        var copied by remember(invitation.code) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::dismissCreatedInvitation,
            title = { Text("Invitation ready") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Share this code privately with someone you trust. It expires in about six days.")
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(invitation.code, fontWeight = FontWeight.Bold)
                    }
                    if (copied) Text("Code copied — ready to paste.", color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(onClick = { clipboard.setText(AnnotatedString(invitation.code)); copied = true }) {
                    Text(if (copied) "Copy again" else "Copy private code")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissCreatedInvitation) { Text("Done") } }
        )
    }
    BackHandler(onBack = onBack)
    removing?.let { invitation ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove this connection?") },
            text = { Text("This also cancels a pending invitation. Your SMS contacts will not change.") },
            confirmButton = { TextButton(onClick = { removing = null; viewModel.revoke(invitation.code) }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Keep") } })
    }
    Column(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(SafeColors.Background, SafeColors.Background)))
        .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("App contacts", style = MaterialTheme.typography.headlineMedium,
            color = SafeColors.Ink, fontWeight = FontWeight.Bold)
        Text("Connect with people you trust.", color = SafeColors.Muted)
        ContactCard {
            Text("SMS delivers your SOS", fontWeight = FontWeight.Bold)
            Text("App notifications are on hold. SOS sends only to the phone numbers in your SMS contacts.")
        }
        val user = state.user
        when {
            user == null -> {
                Text("Sign in to connect with other SafeSignal users.")
                Button(onClick = onAccount) { Text("Open account") }
            }
            !user.emailVerified || user.displayName.isBlank() -> {
                Text("Verify your email and save your profile name before using app contacts.")
                Button(onClick = onAccount) { Text("Open account") }
            }
            else -> {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(state.status, color = SafeColors.Muted)
                OutlinedButton(onClick = viewModel::refresh, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh contacts")
                }
                ContactCard {
                    Text("Invite someone to support you", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Create a private code for someone you trust. App alert delivery is not active yet.")
                    Button(onClick = viewModel::create, enabled = state.ready && !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("Create invitation") }
                }
                ContactCard {
                    Text("Have an invitation?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = state.codeDraft, onValueChange = viewModel::updateCode,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Paste invitation code") },
                        enabled = !state.busy, singleLine = true)
                    Button(onClick = viewModel::preview, enabled = state.ready && !state.busy && state.codeDraft.isNotBlank()) { Text("Preview invitation") }
                    state.preview?.let { invitation ->
                        Text("${invitation.fromName} wants to choose you as an app emergency contact.", fontWeight = FontWeight.Bold)
                        Text("Accept only if this is the person you expected. This does not add them as someone who receives your alerts.")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { viewModel.respond(true) }, enabled = state.ready && !state.busy) { Text("Accept") }
                            OutlinedButton(onClick = { viewModel.respond(false) }, enabled = state.ready && !state.busy) { Text("Decline") }
                        }
                    }
                }
                val visible = state.invitations.filter { it.status != InvitationStatus.REVOKED }
                Text("People you’ve chosen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val outgoing = visible.filter { it.fromUid == user.id }
                if (outgoing.isEmpty()) Text("No app contacts or invitations yet.")
                outgoing.forEach { invitation ->
                    ContactCard {
                        Text(if (invitation.toName.isNotBlank()) invitation.toName else "Private invitation", fontWeight = FontWeight.Bold)
                        val expired = invitation.expiresAtMillis <= System.currentTimeMillis()
                        Text(when (invitation.status) {
                            InvitationStatus.PENDING -> if (expired) "Expired — create another invitation" else "Waiting for acceptance"
                            InvitationStatus.ACCEPTED -> "Accepted — ready for future app alerts"
                            InvitationStatus.DECLINED -> "Invitation declined"
                            InvitationStatus.REVOKED -> "Removed"
                        })
                        if (invitation.status == InvitationStatus.PENDING && !expired) {
                            TextButton(onClick = { clipboard.setText(AnnotatedString(invitation.code)) }, enabled = state.ready && !state.busy) {
                                Text("Copy private code")
                            }
                        }
                        TextButton(onClick = { removing = invitation }, enabled = state.ready && !state.busy) { Text("Remove") }
                    }
                }
                Text("People you support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val incoming = visible.filter { it.toUid == user.id && it.status == InvitationStatus.ACCEPTED }
                if (incoming.isEmpty()) Text("You haven’t accepted anyone’s invitation yet.")
                incoming.forEach { invitation ->
                    ContactCard {
                        Text(invitation.fromName, fontWeight = FontWeight.Bold)
                        Text("You accepted their invitation. App alert delivery is not active yet.")
                        TextButton(onClick = { removing = invitation }, enabled = state.ready && !state.busy) { Text("Remove") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
