package com.example.emergencybutton.data

import com.example.emergencybutton.domain.*

class UnavailableAppContactsRepository : AppContactsRepository {
    private fun failure() = IllegalStateException("App contacts are not connected in this build yet.")
    override fun prepare(user: AccountUser, done: (Result<Unit>) -> Unit) = done(Result.failure(failure()))
    override fun observe(uid: String, changed: (Result<List<ContactInvitation>>) -> Unit): AccountSubscription {
        changed(Result.failure(failure()))
        return AccountSubscription {}
    }
    override fun create(user: AccountUser, done: (Result<ContactInvitation>) -> Unit) = done(Result.failure(failure()))
    override fun preview(code: String, done: (Result<ContactInvitation>) -> Unit) = done(Result.failure(failure()))
    override fun respond(user: AccountUser, code: String, accept: Boolean, done: (Result<Unit>) -> Unit) = done(Result.failure(failure()))
    override fun revoke(uid: String, code: String, done: (Result<Unit>) -> Unit) = done(Result.failure(failure()))
}
