package com.example.emergencybutton.domain

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED, REVOKED }

data class ContactInvitation(
    val code: String,
    val fromUid: String,
    val fromName: String,
    val toUid: String,
    val toName: String,
    val status: InvitationStatus,
    val expiresAtMillis: Long
)

interface AppContactsRepository {
    fun prepare(user: AccountUser, done: (Result<Unit>) -> Unit)
    fun observe(uid: String, changed: (Result<List<ContactInvitation>>) -> Unit): AccountSubscription
    fun create(user: AccountUser, done: (Result<ContactInvitation>) -> Unit)
    fun preview(code: String, done: (Result<ContactInvitation>) -> Unit)
    fun respond(user: AccountUser, code: String, accept: Boolean, done: (Result<Unit>) -> Unit)
    fun revoke(uid: String, code: String, done: (Result<Unit>) -> Unit)
}
