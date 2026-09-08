package com.example.emergencybutton.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.emergencybutton.domain.AccountRepository
import com.example.emergencybutton.domain.AccountResult
import com.example.emergencybutton.domain.AccountUser

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {
    var state by mutableStateOf(AccountUiState(
        configured = repository.isConfigured,
        user = repository.currentUser,
        nameDraft = repository.currentUser?.displayName.orEmpty()
    ))
        private set
    private var disposed = false
    private val subscription = repository.observeUser { user ->
        if (!disposed) synchronizeUser(user)
    }

    fun updateEmail(value: String) { if (!state.busy) state = state.copy(email = value, status = "") }
    fun updatePassword(value: String) { if (!state.busy) state = state.copy(password = value, status = "") }
    fun updateConfirmation(value: String) { if (!state.busy) state = state.copy(confirmation = value, status = "") }
    fun updateName(value: String) { if (!state.busy) state = state.copy(nameDraft = value.take(80), status = "") }

    fun showForm(form: AccountForm) {
        if (!state.busy) state = state.copy(form = form, password = "", confirmation = "", status = "", isError = false)
    }

    fun leaveScreen() {
        state = state.copy(password = "", confirmation = "")
    }

    fun submit() {
        if (state.busy || state.user != null) return
        if (!state.configured) {
            error("Accounts are not available in this build yet. You can still use SOS.")
            return
        }
        val email = state.email.trim()
        if (!EMAIL.matches(email)) {
            error("Enter a valid email address.")
            return
        }
        if (state.form == AccountForm.RESET_PASSWORD) {
            perform(RESET_MESSAGE) { repository.resetPassword(email, it) }
            return
        }
        val password = state.password
        if (password.isEmpty()) {
            error("Enter your password.")
            return
        }
        if (state.form == AccountForm.REGISTER) {
            if (password.length < 8) {
                error("Choose a password with at least 8 characters.")
                return
            }
            if (password != state.confirmation) {
                error("The passwords do not match.")
                return
            }
            perform("Account created. You can verify your email below.") { repository.register(email, password, it) }
        } else {
            perform("You are signed in.") { repository.signIn(email, password, it) }
        }
    }

    fun saveName() {
        if (state.busy || state.user == null) return
        val name = state.nameDraft.trim()
        if (name.isEmpty()) {
            error("Enter the name you would like to use.")
            return
        }
        perform("Your name has been updated.") { repository.updateName(name, it) }
    }

    fun sendVerificationEmail() {
        if (state.busy || state.user == null || state.user?.emailVerified == true) return
        perform("Verification email sent. Open its link, then tap Check verification.") {
            repository.sendVerificationEmail(it)
        }
    }

    fun refreshUser() {
        if (state.busy || state.user == null) return
        perform("Account refreshed.") { repository.refreshUser(it) }
    }

    fun resetSignedInPassword() {
        val email = state.user?.email ?: return
        if (!state.busy) perform(RESET_MESSAGE) { repository.resetPassword(email, it) }
    }

    fun signOut() {
        if (state.busy || state.user == null) return
        perform("Signed out. SOS is still available on this phone.") { repository.signOut(it) }
    }

    private fun perform(successMessage: String, operation: ((AccountResult) -> Unit) -> Unit) {
        if (state.busy || disposed) return
        state = state.copy(busy = true, status = "", isError = false)
        operation { result ->
            if (disposed) return@operation
            synchronizeUser(repository.currentUser)
            state = when (result) {
                AccountResult.Success -> state.copy(
                    busy = false, password = "", confirmation = "", isError = false,
                    nameDraft = state.user?.displayName.orEmpty(), status = successMessage
                )
                is AccountResult.Failure -> state.copy(
                    busy = false, password = "", confirmation = "", isError = true, status = result.message
                )
            }
        }
    }

    private fun synchronizeUser(user: AccountUser?) {
        val changed = state.user?.id != user?.id
        state = state.copy(
            user = user,
            nameDraft = if (changed) user?.displayName.orEmpty() else state.nameDraft,
            password = if (changed) "" else state.password,
            confirmation = if (changed) "" else state.confirmation,
            form = if (changed) AccountForm.SIGN_IN else state.form
        )
    }

    private fun error(message: String) { state = state.copy(status = message, isError = true) }

    override fun onCleared() {
        disposed = true
        subscription.cancel()
        state = state.copy(password = "", confirmation = "")
        super.onCleared()
    }

    companion object {
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private const val RESET_MESSAGE = "If an account uses this email, a password reset link will be sent. Check your inbox and spam folder."
    }
}
