package com.example.emergencybutton.data

import com.example.emergencybutton.domain.AccountSubscription
import com.example.emergencybutton.domain.AccountUser
import com.example.emergencybutton.domain.AppContactsRepository
import com.example.emergencybutton.domain.ContactInvitation
import com.example.emergencybutton.domain.InvitationStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import java.util.Date
import java.util.UUID

class FirestoreAppContactsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AppContactsRepository {
    private val invitations get() = db.collection("contactInvitations")

    override fun prepare(user: AccountUser, done: (Result<Unit>) -> Unit) {
        val signedIn = auth.currentUser
        if (signedIn?.uid != user.id || !signedIn.isEmailVerified) {
            done(Result.failure(IllegalStateException("Sign in and verify your email first.")))
            return
        }
        // Reload the token so rules see email_verified immediately after browser verification.
        signedIn.getIdToken(true).addOnCompleteListener { token ->
            if (!token.isSuccessful) {
                done(Result.failure(friendly(token.exception)))
            } else {
                db.runTransaction { transaction ->
                    checkSession(user.id)
                    val ref = db.collection("users").document(user.id)
                    transaction.get(ref)
                    transaction.set(ref, mapOf("displayName" to user.displayName.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()))
                }.addOnCompleteListener { done(it.toUnitResult()) }
            }
        }
    }

    override fun observe(uid: String, changed: (Result<List<ContactInvitation>>) -> Unit): AccountSubscription {
        val records = mutableMapOf<String, List<ContactInvitation>>()
        val serverReady = mutableSetOf<String>()
        val listeners = listOf("fromUid", "toUid").map { field ->
            invitations.whereEqualTo(field, uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (auth.currentUser?.uid != uid) return@addSnapshotListener
                if (error != null) {
                    serverReady.remove(field)
                    changed(Result.failure(friendly(error)))
                } else if (snapshot != null) {
                    // Do not present cached membership as confirmation from the server.
                    if (snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()) {
                        serverReady.remove(field)
                        changed(Result.failure(IllegalStateException("Connecting to app contacts. Check your internet connection if this continues.")))
                    } else {
                        records[field] = snapshot.documents.mapNotNull { it.toInvitation() }
                        serverReady.add(field)
                        if (serverReady.size == 2) changed(Result.success(records.values.flatten().distinctBy { it.code }))
                    }
                }
            }
        }
        return AccountSubscription { listeners.forEach { it.remove() } }
    }

    override fun create(user: AccountUser, done: (Result<ContactInvitation>) -> Unit) {
        val code = UUID.randomUUID().toString().replace("-", "")
        // Six days gives modest clock-skew tolerance under the rules' seven-day maximum.
        val expires = System.currentTimeMillis() + 6L * 24 * 60 * 60 * 1000
        val invitation = ContactInvitation(code, user.id, user.displayName.trim(), "", "", InvitationStatus.PENDING, expires)
        db.runTransaction { transaction ->
            checkSession(user.id)
            val ref = invitations.document(code)
            check(!transaction.get(ref).exists()) { "Please try creating the invitation again." }
            transaction.set(ref, mapOf(
                "fromUid" to user.id, "fromName" to invitation.fromName, "toUid" to "", "toName" to "",
                "status" to "pending", "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(), "expiresAt" to Timestamp(Date(expires))
            ))
        }.addOnCompleteListener { task ->
            done(if (task.isSuccessful) Result.success(invitation) else Result.failure(friendly(task.exception)))
        }
    }

    override fun preview(code: String, done: (Result<ContactInvitation>) -> Unit) {
        invitations.document(code).get(Source.SERVER).addOnCompleteListener { task ->
            val invitation = if (task.isSuccessful) task.result?.toInvitation() else null
            done(if (invitation != null) Result.success(invitation)
                else Result.failure(IllegalStateException("This code is unavailable, expired, or already used. Ask for a new invitation.")))
        }
    }

    override fun respond(user: AccountUser, code: String, accept: Boolean, done: (Result<Unit>) -> Unit) {
        db.runTransaction { transaction ->
            checkSession(user.id)
            val ref = invitations.document(code)
            val invitation = transaction.get(ref).toInvitation()
            check(invitation != null && invitation.fromUid != user.id && invitation.status == InvitationStatus.PENDING) {
                "This invitation is no longer available."
            }
            transaction.update(ref, mapOf("toUid" to user.id, "toName" to user.displayName.trim(),
                "status" to if (accept) "accepted" else "declined", "updatedAt" to FieldValue.serverTimestamp()))
        }.addOnCompleteListener { done(it.toUnitResult()) }
    }

    override fun revoke(uid: String, code: String, done: (Result<Unit>) -> Unit) {
        db.runTransaction { transaction ->
            checkSession(uid)
            val ref = invitations.document(code)
            val invitation = transaction.get(ref).toInvitation()
            check(invitation != null && (invitation.fromUid == uid || invitation.toUid == uid)) {
                "This connection is not available."
            }
            transaction.update(ref, mapOf("status" to "revoked", "updatedAt" to FieldValue.serverTimestamp()))
        }.addOnCompleteListener { done(it.toUnitResult()) }
    }

    private fun checkSession(uid: String) { check(auth.currentUser?.uid == uid) { "Your account changed. Open app contacts again." } }

    private fun DocumentSnapshot.toInvitation(): ContactInvitation? {
        val status = when (getString("status")) {
            "pending" -> InvitationStatus.PENDING
            "accepted" -> InvitationStatus.ACCEPTED
            "declined" -> InvitationStatus.DECLINED
            "revoked" -> InvitationStatus.REVOKED
            else -> return null
        }
        return ContactInvitation(id, getString("fromUid") ?: return null, getString("fromName").orEmpty(),
            getString("toUid").orEmpty(), getString("toName").orEmpty(), status,
            getTimestamp("expiresAt")?.toDate()?.time ?: return null)
    }

    private fun com.google.android.gms.tasks.Task<*>.toUnitResult(): Result<Unit> =
        if (isSuccessful) Result.success(Unit) else Result.failure(friendly(exception))

    private fun friendly(error: Exception?): Exception = IllegalStateException(when {
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "App contacts are unavailable or this invitation changed. Check email verification and ask the app owner to check database setup."
        error is FirebaseFirestoreException && error.code in listOf(FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) ->
            "Could not reach app contacts. Check your internet connection and try again."
        else -> "Could not update app contacts. Check your connection and try again."
    })
}
