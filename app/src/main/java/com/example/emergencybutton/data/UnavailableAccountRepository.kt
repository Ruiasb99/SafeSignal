package com.example.emergencybutton.data

import com.example.emergencybutton.domain.AccountRepository
import com.example.emergencybutton.domain.AccountResult
import com.example.emergencybutton.domain.AccountSubscription
import com.example.emergencybutton.domain.AccountUser

/** Explicitly unavailable, never a fake successful login, when configuration is absent. */
class UnavailableAccountRepository : AccountRepository {
    override val isConfigured = false
    override val currentUser: AccountUser? = null
    override fun observeUser(onChange: (AccountUser?) -> Unit): AccountSubscription {
        onChange(null)
        return AccountSubscription {}
    }
    override fun register(email: String, password: String, onResult: (AccountResult) -> Unit) = unavailable(onResult)
    override fun signIn(email: String, password: String, onResult: (AccountResult) -> Unit) = unavailable(onResult)
    override fun resetPassword(email: String, onResult: (AccountResult) -> Unit) = unavailable(onResult)
    override fun updateName(name: String, onResult: (AccountResult) -> Unit) = unavailable(onResult)
    override fun sendVerificationEmail(onResult: (AccountResult) -> Unit) = unavailable(onResult)
    override fun refreshUser(onResult: (AccountResult) -> Unit) = unavailable(onResult)
    override fun signOut(onResult: (AccountResult) -> Unit) = unavailable(onResult)
    private fun unavailable(callback: (AccountResult) -> Unit) {
        callback(AccountResult.Failure("Accounts are not available in this build yet. You can still use SOS."))
    }
}
