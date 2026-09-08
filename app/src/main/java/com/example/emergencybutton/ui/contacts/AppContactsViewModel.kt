package com.example.emergencybutton.ui.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.emergencybutton.domain.*

data class AppContactsState(
    val user: AccountUser? = null,
    val ready: Boolean = false,
    val busy: Boolean = false,
    val codeDraft: String = "",
    val preview: ContactInvitation? = null,
    val createdInvitation: ContactInvitation? = null,
    val invitations: List<ContactInvitation> = emptyList(),
    val status: String = ""
)

class AppContactsViewModel(private val repository: AppContactsRepository) : ViewModel() {
    var state by mutableStateOf(AppContactsState())
        private set
    private var generation = 0L
    private var subscription: AccountSubscription? = null

    fun bind(user: AccountUser?) {
        generation++
        subscription?.cancel()
        subscription = null
        state = AppContactsState(user = user)
        if (user == null || !user.emailVerified || user.displayName.isBlank()) return
        val session = generation
        state = state.copy(busy = true, status = "Connecting to app contacts…")
        repository.prepare(user) { result ->
            if (session != generation) return@prepare
            if (result.isFailure) {
                state = state.copy(busy = false, status = result.exceptionOrNull()?.message.orEmpty())
            } else {
                subscription = repository.observe(user.id) { update ->
                    if (session != generation) return@observe
                    update.fold(
                        onSuccess = { state = state.copy(ready = true, invitations = it, status = "App contacts are up to date.") },
                        onFailure = { state = state.copy(ready = false, invitations = emptyList(), preview = null, createdInvitation = null, status = it.message.orEmpty()) }
                    )
                }
                state = state.copy(busy = false)
            }
        }
    }

    fun refresh() { if (!state.busy) bind(state.user) }
    fun updateCode(value: String) {
        if (!state.busy) state = state.copy(codeDraft = value, preview = null)
    }

    fun dismissCreatedInvitation() { state = state.copy(createdInvitation = null) }

    fun create() {
        val user = activeUser() ?: return
        val session = generation
        state = state.copy(busy = true)
        repository.create(user) { result ->
            if (session != generation) return@create
            state = state.copy(busy = false, createdInvitation = result.getOrNull(), status = result.fold(
                { "Invitation created. Share its private code with someone you trust." },
                { it.message.orEmpty() }))
        }
    }

    fun preview() {
        val user = activeUser() ?: return
        val code = state.codeDraft.trim().lowercase()
        if (!CODE.matches(code)) {
            state = state.copy(status = "Paste the complete 32-character invitation code.")
            return
        }
        val session = generation
        state = state.copy(busy = true, preview = null)
        repository.preview(code) { result ->
            if (session != generation) return@preview
            val invitation = result.getOrNull()
            val alreadyConnected = state.invitations.any {
                it.status == InvitationStatus.ACCEPTED && it.fromUid == invitation?.fromUid && it.toUid == user.id
            }
            val valid = invitation != null && invitation.fromUid != user.id &&
                !alreadyConnected && invitation.status == InvitationStatus.PENDING && invitation.expiresAtMillis > System.currentTimeMillis()
            state = state.copy(busy = false, preview = if (valid) invitation else null,
                status = if (valid) "Confirm you know this person before accepting."
                    else if (alreadyConnected) "You already support this person."
                    else result.exceptionOrNull()?.message ?: "This invitation is yours, expired, or already used.")
        }
    }

    fun respond(accept: Boolean) {
        val user = activeUser() ?: return
        val invitation = state.preview ?: return
        val session = generation
        state = state.copy(busy = true)
        repository.respond(user, invitation.code, accept) { result ->
            if (session != generation) return@respond
            state = state.copy(busy = false, preview = null, codeDraft = "", status = result.fold(
                { if (accept) "Invitation accepted. This connection is ready for future app alerts." else "Invitation declined." },
                { it.message.orEmpty() }))
        }
    }

    fun revoke(code: String) {
        val user = activeUser() ?: return
        val invitation = state.invitations.find { it.code == code } ?: return
        if (user.id != invitation.fromUid && user.id != invitation.toUid) return
        val session = generation
        state = state.copy(busy = true)
        repository.revoke(user.id, code) { result ->
            if (session != generation) return@revoke
            state = state.copy(busy = false, status = result.fold({ "Connection removed / invitation cancelled." }, { it.message.orEmpty() }))
        }
    }

    private fun activeUser(): AccountUser? =
        state.user?.takeIf { state.ready && !state.busy && it.emailVerified && it.displayName.isNotBlank() }

    override fun onCleared() {
        bind(null)
        super.onCleared()
    }

    companion object { private val CODE = Regex("^[a-f0-9]{32}$") }
}
