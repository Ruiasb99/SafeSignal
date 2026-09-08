package com.example.emergencybutton.ui.account

import com.example.emergencybutton.ui.theme.SafeColors
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private val AccountInk = SafeColors.Ink
private val AccountMuted = SafeColors.Muted
private val AccountTeal = SafeColors.Primary

@Composable
fun AccountScreen(
    state: AccountUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onFormChange: (AccountForm) -> Unit,
    onSubmit: () -> Unit,
    onSaveName: () -> Unit,
    onSendVerification: () -> Unit,
    onRefresh: () -> Unit,
    onResetPassword: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(SafeColors.Background, SafeColors.Background)))

            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Your account", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = AccountInk)
        Text("Your place in SafeSignal.", color = AccountMuted)

        if (!state.configured) {
            AccountCard(color = Color(0xFFFFEED5)) {
                Text("Accounts are not connected yet", fontWeight = FontWeight.Bold, color = AccountInk)
                Text("Sign-in will be available when account setup is complete. You can explore the forms and keep using SOS.",
                    color = AccountMuted)
            }
        }

        val user = state.user
        if (user == null) {
            AccountCard {
                Text(when (state.form) {
                    AccountForm.SIGN_IN -> "Welcome back"
                    AccountForm.REGISTER -> "Create your account"
                    AccountForm.RESET_PASSWORD -> "Reset your password"
                }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AccountInk)
                if (state.form == AccountForm.RESET_PASSWORD) {
                    Text("Enter your account email and we’ll send a reset link.", color = AccountMuted)
                }
                OutlinedTextField(
                    value = state.email, onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                    label = { Text("Email address") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp)
                )
                if (state.form != AccountForm.RESET_PASSWORD) {
                    PasswordField(state.password, onPasswordChange, "Password", !state.busy, state.form)
                }
                if (state.form == AccountForm.REGISTER) {
                    PasswordField(state.confirmation, onConfirmationChange, "Confirm password", !state.busy, state.form)
                    Text("Use at least 8 characters. Your account password is separate from your cancellation PIN.",
                        style = MaterialTheme.typography.bodySmall, color = AccountMuted)
                }
                AccountButton(
                    text = when (state.form) {
                        AccountForm.SIGN_IN -> "Sign in"
                        AccountForm.REGISTER -> "Create account"
                        AccountForm.RESET_PASSWORD -> "Send reset link"
                    },
                    enabled = state.configured && !state.busy,
                    onClick = onSubmit
                )
                when (state.form) {
                    AccountForm.SIGN_IN -> {
                        TextButton(onClick = { onFormChange(AccountForm.REGISTER) }, enabled = !state.busy) {
                            Text("New here? Create an account", color = AccountTeal)
                        }
                        TextButton(onClick = { onFormChange(AccountForm.RESET_PASSWORD) }, enabled = !state.busy) {
                            Text("Forgot password?", color = AccountTeal)
                        }
                    }
                    else -> TextButton(onClick = { onFormChange(AccountForm.SIGN_IN) }, enabled = !state.busy) {
                        Text("Back to sign in", color = AccountTeal)
                    }
                }
            }
        } else {
            AccountCard {
                Text(user.displayName.ifBlank { "You’re signed in" },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AccountInk)
                Text(user.email, color = AccountMuted)
                Text(if (user.emailVerified) "Email verified" else "Email not verified yet",
                    fontWeight = FontWeight.SemiBold,
                    color = if (user.emailVerified) Color(0xFF138A62) else Color(0xFF9A6100))
                if (!user.emailVerified) {
                    Text("Verify your email to confirm this account belongs to you.", color = AccountMuted)
                    AccountButton("Send verification email", !state.busy, onSendVerification)
                    OutlinedButton(onClick = onRefresh, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Check verification")
                    }
                }
            }
            AccountCard {
                Text("Profile", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = AccountInk)
                OutlinedTextField(
                    value = state.nameDraft, onValueChange = onNameChange, enabled = !state.busy,
                    label = { Text("Your name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                )
                AccountButton("Save name", !state.busy, onSaveName)
                OutlinedButton(onClick = onResetPassword, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Email password reset link")
                }
            }
        }

        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AccountTeal)
            Text("Working on your account…", color = AccountMuted)
        }
        if (state.status.isNotEmpty()) {
            AccountCard(color = if (state.isError) Color(0xFFFFE8E6) else SafeColors.Navigation) {
                Text(state.status, color = if (state.isError) Color(0xFF9F1C1C) else AccountInk)
            }
        }
        Text("Your emergency contacts and cancellation PIN stay on this phone. Account changes do not stop an active alert. SOS works without signing in.",
            style = MaterialTheme.typography.bodySmall, color = AccountMuted)
        if (user != null) {
            OutlinedButton(onClick = onSignOut, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Sign out", color = AccountTeal)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AccountCard(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun AccountButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AccountTeal)) {
        Text(text)
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean, form: AccountForm) {
    var visible by remember(form) { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, enabled = enabled, singleLine = true,
        modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") }
        },
        shape = RoundedCornerShape(14.dp)
    )
}
