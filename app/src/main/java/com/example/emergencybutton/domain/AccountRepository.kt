package com.example.emergencybutton.domain

data class AccountUser(
    val id: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean
)

sealed interface AccountResult {
    data object Success : AccountResult
    data class Failure(val message: String) : AccountResult
}

fun interface AccountSubscription {
    fun cancel()
}

/** All callbacks arrive on the main thread. Passwords must never be persisted by the app. */
interface AccountRepository {
    val isConfigured: Boolean
    val currentUser: AccountUser?
    fun observeUser(onChange: (AccountUser?) -> Unit): AccountSubscription
    fun register(email: String, password: String, onResult: (AccountResult) -> Unit)
    fun signIn(email: String, password: String, onResult: (AccountResult) -> Unit)
    fun resetPassword(email: String, onResult: (AccountResult) -> Unit)
    fun updateName(name: String, onResult: (AccountResult) -> Unit)
    fun sendVerificationEmail(onResult: (AccountResult) -> Unit)
    fun refreshUser(onResult: (AccountResult) -> Unit)
    fun signOut(onResult: (AccountResult) -> Unit)
}
