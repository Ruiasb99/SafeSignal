package com.example.emergencybutton.data

import com.example.emergencybutton.domain.AccountRepository
import com.example.emergencybutton.domain.AccountResult
import com.example.emergencybutton.domain.AccountSubscription
import com.example.emergencybutton.domain.AccountUser
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class FirebaseAccountRepository(private val auth: FirebaseAuth) : AccountRepository {
    override val isConfigured = true
    override val currentUser: AccountUser? get() = auth.currentUser?.toAccountUser()

    init { auth.useAppLanguage() }

    override fun observeUser(onChange: (AccountUser?) -> Unit): AccountSubscription {
        val listener = FirebaseAuth.AuthStateListener { onChange(it.currentUser?.toAccountUser()) }
        auth.addAuthStateListener(listener)
        return AccountSubscription { auth.removeAuthStateListener(listener) }
    }

    override fun register(email: String, password: String, onResult: (AccountResult) -> Unit) =
        execute(onResult) { auth.createUserWithEmailAndPassword(email, password) }

    override fun signIn(email: String, password: String, onResult: (AccountResult) -> Unit) =
        execute(onResult) { auth.signInWithEmailAndPassword(email, password) }

    override fun resetPassword(email: String, onResult: (AccountResult) -> Unit) {
        // Do not reveal whether an email has an account, including on older Firebase projects.
        execute(onResult, hideMissingUser = true) { auth.sendPasswordResetEmail(email) }
    }

    override fun updateName(name: String, onResult: (AccountResult) -> Unit) {
        val user = requireUser(onResult) ?: return
        execute(onResult) {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
        }
    }

    override fun sendVerificationEmail(onResult: (AccountResult) -> Unit) {
        val user = requireUser(onResult) ?: return
        execute(onResult) { user.sendEmailVerification() }
    }

    override fun refreshUser(onResult: (AccountResult) -> Unit) {
        val user = requireUser(onResult) ?: return
        execute(onResult) { user.reload() }
    }

    override fun signOut(onResult: (AccountResult) -> Unit) {
        try {
            auth.signOut()
            onResult(AccountResult.Success)
        } catch (error: Exception) {
            onResult(failure(error))
        }
    }

    private fun requireUser(callback: (AccountResult) -> Unit): FirebaseUser? {
        val user = auth.currentUser
        if (user == null) callback(AccountResult.Failure("Please sign in again."))
        return user
    }

    private fun execute(
        onResult: (AccountResult) -> Unit,
        hideMissingUser: Boolean = false,
        operation: () -> Task<*>
    ) {
        try {
            operation().addOnCompleteListener { task ->
                val missingUser = (task.exception as? FirebaseAuthException)?.errorCode == "ERROR_USER_NOT_FOUND"
                onResult(if (task.isSuccessful || (hideMissingUser && missingUser)) {
                    AccountResult.Success
                } else failure(task.exception))
            }
        } catch (error: Exception) {
            onResult(failure(error))
        }
    }

    private fun failure(error: Exception?): AccountResult.Failure {
        val message = when {
            error is FirebaseNetworkException -> "Could not connect. Check your internet connection and try again."
            error is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a little before trying again."
            else -> when ((error as? FirebaseAuthException)?.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                "ERROR_WEAK_PASSWORD" -> "That password does not meet the account password requirements. Try a stronger password."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Could not create an account with these details. Try signing in or resetting your password."
                "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS",
                "ERROR_USER_DISABLED" -> "Could not sign in with those details. Check your email and password or reset your password."
                "ERROR_REQUIRES_RECENT_LOGIN", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" ->
                    "Your session needs to be renewed. Sign out and sign in again."
                "ERROR_OPERATION_NOT_ALLOWED", "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_API_KEY", "ERROR_API_NOT_AVAILABLE" ->
                    "Account access is not available right now. Please try again later."
                else -> "The account request could not be completed. Please try again."
            }
        }
        return AccountResult.Failure(message)
    }

    private fun FirebaseUser.toAccountUser() = AccountUser(uid, email.orEmpty(), displayName.orEmpty(), isEmailVerified)
}
