package com.example.emergencybutton

import androidx.lifecycle.ViewModelStore
import com.example.emergencybutton.data.UnavailableAccountRepository
import com.example.emergencybutton.domain.AccountRepository
import com.example.emergencybutton.domain.AccountResult
import com.example.emergencybutton.domain.AccountSubscription
import com.example.emergencybutton.domain.AccountUser
import com.example.emergencybutton.ui.account.AccountForm
import com.example.emergencybutton.ui.account.AccountViewModel
import org.junit.Assert.*
import org.junit.Test

class AccountViewModelTest {
    private val user = AccountUser("user-one", "test@example.com", "Test", false)

    @Test fun unconfiguredBuildDoesNotPretendToSignIn() {
        val vm = AccountViewModel(UnavailableAccountRepository())
        vm.updateEmail("test@example.com")
        vm.updatePassword("password123")
        vm.submit()
        assertFalse(vm.state.configured)
        assertNull(vm.state.user)
        assertFalse(vm.state.busy)
        assertTrue(vm.state.isError)
    }

    @Test fun existingSessionIsRestoredWithoutNewSignIn() {
        val repository = FakeAccounts(user)
        val vm = AccountViewModel(repository)
        assertEquals(user, vm.state.user)
        assertEquals("Test", vm.state.nameDraft)
        assertTrue(repository.calls.isEmpty())
    }

    @Test fun invalidEmailAndMissingPasswordNeverReachBackend() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.updateEmail("not-an-email")
        vm.updatePassword("password123")
        vm.submit()
        assertTrue(vm.state.isError)
        vm.updateEmail("test@example.com")
        vm.updatePassword("")
        vm.submit()
        assertEquals("Enter your password.", vm.state.status)
        assertTrue(repository.calls.isEmpty())
    }

    @Test fun registrationRequiresLongEnoughMatchingPasswords() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.showForm(AccountForm.REGISTER)
        vm.updateEmail("test@example.com")
        vm.updatePassword("short")
        vm.updateConfirmation("short")
        vm.submit()
        assertTrue(vm.state.status.contains("8 characters"))
        vm.updatePassword("password123")
        vm.updateConfirmation("different123")
        vm.submit()
        assertEquals("The passwords do not match.", vm.state.status)
        assertTrue(repository.calls.isEmpty())
    }

    @Test fun registrationTrimsEmailButPreservesPasswordExactly() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.showForm(AccountForm.REGISTER)
        vm.updateEmail(" test@example.com ")
        vm.updatePassword(" password123 ")
        vm.updateConfirmation(" password123 ")
        vm.submit()
        assertEquals("register", repository.calls.single())
        assertEquals("test@example.com", repository.lastEmail)
        assertEquals(" password123 ", repository.lastPassword)
        repository.finish(user = user)
        assertEquals(user, vm.state.user)
        assertEquals("", vm.state.password)
        assertEquals("", vm.state.confirmation)
        assertFalse(vm.state.busy)
    }

    @Test fun signInDoesNotApplyNewAccountPasswordLengthPolicy() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.updateEmail("test@example.com")
        vm.updatePassword("sixsix")
        vm.submit()
        assertEquals("signIn", repository.calls.single())
    }

    @Test fun repeatedTapsAndEditsAreBlockedDuringAnAccountRequest() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.updateEmail("test@example.com")
        vm.updatePassword("password123")
        vm.submit()
        vm.submit()
        vm.updateEmail("other@example.com")
        vm.showForm(AccountForm.REGISTER)
        assertEquals(1, repository.calls.size)
        assertEquals("test@example.com", vm.state.email)
        assertEquals(AccountForm.SIGN_IN, vm.state.form)
        assertTrue(vm.state.busy)
    }

    @Test fun failureClearsPasswordAndAllowsRetry() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.updateEmail("test@example.com")
        vm.updatePassword("wrong-password")
        vm.submit()
        repository.finish(AccountResult.Failure("Could not connect."))
        assertFalse(vm.state.busy)
        assertTrue(vm.state.isError)
        assertEquals("Could not connect.", vm.state.status)
        assertEquals("", vm.state.password)
        vm.updatePassword("correct-password")
        vm.submit()
        assertEquals(2, repository.calls.size)
    }

    @Test fun passwordResetUsesNeutralConfirmationWithoutSigningIn() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.showForm(AccountForm.RESET_PASSWORD)
        vm.updateEmail("test@example.com")
        vm.submit()
        assertEquals("resetPassword", repository.calls.single())
        repository.finish()
        assertTrue(vm.state.status.startsWith("If an account uses this email"))
        assertNull(vm.state.user)
    }

    @Test fun signedInResetUsesAccountEmailNotDraftEmail() {
        val repository = FakeAccounts(user)
        val vm = AccountViewModel(repository)
        vm.updateEmail("unrelated@example.com")
        vm.resetSignedInPassword()
        assertEquals(user.email, repository.lastEmail)
    }

    @Test fun profileNameIsValidatedAndUpdatedAfterServerSuccess() {
        val repository = FakeAccounts(user)
        val vm = AccountViewModel(repository)
        vm.updateName(" ")
        vm.saveName()
        assertTrue(repository.calls.isEmpty())
        vm.updateName(" New Name ")
        vm.saveName()
        assertEquals("New Name", repository.lastName)
        assertEquals("Test", vm.state.user?.displayName)
        repository.finish(user = user.copy(displayName = "New Name"))
        assertEquals("New Name", vm.state.user?.displayName)
        assertEquals("New Name", vm.state.nameDraft)
    }

    @Test fun verificationRemainsUnverifiedUntilRefreshedFromServer() {
        val repository = FakeAccounts(user)
        val vm = AccountViewModel(repository)
        vm.sendVerificationEmail()
        repository.finish()
        assertFalse(vm.state.user!!.emailVerified)
        vm.refreshUser()
        repository.finish(user = user.copy(emailVerified = true))
        assertTrue(vm.state.user!!.emailVerified)
        vm.sendVerificationEmail()
        assertEquals(listOf("verify", "refresh"), repository.calls)
    }

    @Test fun signOutClearsUserAndSensitiveDrafts() {
        val repository = FakeAccounts(user)
        val vm = AccountViewModel(repository)
        vm.signOut()
        repository.finish(user = null)
        assertNull(vm.state.user)
        assertEquals("", vm.state.password)
        assertEquals("", vm.state.nameDraft)
        assertTrue(vm.state.status.contains("SOS is still available"))
    }

    @Test fun leavingScreenClearsPasswordButPendingOperationCanFinish() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        vm.updateEmail("test@example.com")
        vm.updatePassword("password123")
        vm.submit()
        vm.leaveScreen()
        assertEquals("", vm.state.password)
        repository.finish(user = user)
        assertEquals(user, vm.state.user)
    }

    @Test fun switchingFormsClearsBothPasswords() {
        val vm = AccountViewModel(FakeAccounts())
        vm.showForm(AccountForm.REGISTER)
        vm.updatePassword("password123")
        vm.updateConfirmation("password123")
        vm.showForm(AccountForm.SIGN_IN)
        assertEquals("", vm.state.password)
        assertEquals("", vm.state.confirmation)
    }

    @Test fun authListenerUpdatesSessionWithoutOverwritingAnInProgressNameEdit() {
        val repository = FakeAccounts(user)
        val vm = AccountViewModel(repository)
        vm.updateName("Draft Name")
        repository.emit(user.copy(emailVerified = true))
        assertTrue(vm.state.user!!.emailVerified)
        assertEquals("Draft Name", vm.state.nameDraft)
        repository.emit(null)
        assertNull(vm.state.user)
        assertEquals("", vm.state.nameDraft)
    }

    @Test fun clearingViewModelUnsubscribesAndIgnoresLateResults() {
        val repository = FakeAccounts()
        val vm = AccountViewModel(repository)
        val store = ViewModelStore()
        store.put("account", vm)
        vm.updateEmail("test@example.com")
        vm.updatePassword("password123")
        vm.submit()
        store.clear()
        assertTrue(repository.unsubscribed)
        repository.finish(user = user)
        assertNull(vm.state.user)
        assertEquals("", vm.state.password)
    }

    private class FakeAccounts(initial: AccountUser? = null) : AccountRepository {
        override val isConfigured = true
        override var currentUser: AccountUser? = initial
        val calls = mutableListOf<String>()
        var lastEmail = ""
        var lastPassword = ""
        var lastName = ""
        var unsubscribed = false
        private var observer: ((AccountUser?) -> Unit)? = null
        private var pending: ((AccountResult) -> Unit)? = null

        override fun observeUser(onChange: (AccountUser?) -> Unit): AccountSubscription {
            observer = onChange
            onChange(currentUser)
            return AccountSubscription { unsubscribed = true; observer = null }
        }
        override fun register(email: String, password: String, onResult: (AccountResult) -> Unit) {
            lastEmail = email; lastPassword = password; enqueue("register", onResult)
        }
        override fun signIn(email: String, password: String, onResult: (AccountResult) -> Unit) {
            lastEmail = email; lastPassword = password; enqueue("signIn", onResult)
        }
        override fun resetPassword(email: String, onResult: (AccountResult) -> Unit) {
            lastEmail = email; enqueue("resetPassword", onResult)
        }
        override fun updateName(name: String, onResult: (AccountResult) -> Unit) {
            lastName = name; enqueue("name", onResult)
        }
        override fun sendVerificationEmail(onResult: (AccountResult) -> Unit) = enqueue("verify", onResult)
        override fun refreshUser(onResult: (AccountResult) -> Unit) = enqueue("refresh", onResult)
        override fun signOut(onResult: (AccountResult) -> Unit) = enqueue("signOut", onResult)
        private fun enqueue(name: String, callback: (AccountResult) -> Unit) { calls.add(name); pending = callback }
        fun emit(user: AccountUser?) { currentUser = user; observer?.invoke(user) }
        fun finish(result: AccountResult = AccountResult.Success, user: AccountUser? = currentUser) {
            currentUser = user
            val callback = pending
            pending = null
            observer?.invoke(currentUser)
            callback!!(result)
        }
    }
}
