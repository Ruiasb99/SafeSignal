package com.example.emergencybutton

import com.example.emergencybutton.domain.*
import com.example.emergencybutton.ui.contacts.AppContactsViewModel
import org.junit.Assert.*
import org.junit.Test

class AppContactsViewModelTest {
    private val alice = AccountUser("alice", "alice@example.com", "Alice", true)
    private val bob = AccountUser("bob", "bob@example.com", "Bob", true)
    private val invite = ContactInvitation("a".repeat(32), "alice", "Alice", "", "", InvitationStatus.PENDING,
        System.currentTimeMillis() + 86_400_000)

    @Test fun signedOutUnverifiedOrUnnamedUserCannotStartCloudOperations() {
        val repo = FakeContacts()
        val vm = AppContactsViewModel(repo)
        for (user in listOf(null, alice.copy(emailVerified = false), alice.copy(displayName = ""))) {
            vm.bind(user); vm.create(); vm.preview(); vm.respond(true)
        }
        assertEquals(0, repo.prepared.size)
        assertEquals(0, repo.creates)
    }

    @Test fun preparationFailureShowsErrorAndBlocksActions() {
        val repo = FakeContacts()
        val vm = AppContactsViewModel(repo)
        vm.bind(alice)
        repo.prepared.single()(Result.failure(Exception("Database not ready")))
        vm.create()
        assertFalse(vm.state.ready)
        assertFalse(vm.state.busy)
        assertEquals("Database not ready", vm.state.status)
        assertEquals(0, repo.creates)
    }

    @Test fun serverReadinessIsRequiredAndListenerFailureClearsPrivateLists() {
        val repo = FakeContacts()
        val vm = ready(repo, alice)
        repo.observers.single()(Result.success(listOf(invite)))
        assertEquals(1, vm.state.invitations.size)
        repo.observers.single()(Result.failure(Exception("Offline")))
        assertFalse(vm.state.ready)
        assertTrue(vm.state.invitations.isEmpty())
        vm.create()
        assertEquals(0, repo.creates)
    }

    @Test fun changingAccountClearsDataAndIgnoresOldPreparationAndListenerCallbacks() {
        val repo = FakeContacts()
        val vm = ready(repo, alice)
        repo.observers[0](Result.success(listOf(invite)))
        vm.updateCode(invite.code)
        vm.bind(bob)
        assertTrue(repo.cancelled)
        assertTrue(vm.state.invitations.isEmpty())
        assertEquals("", vm.state.codeDraft)
        repo.observers[0](Result.success(listOf(invite)))
        assertTrue(vm.state.invitations.isEmpty())
        repo.prepared[0](Result.success(Unit))
        assertEquals(1, repo.observers.size)
    }

    @Test fun malformedCodeIsNotLookedUpAndOwnOrExpiredInvitesCannotBeAccepted() {
        val repo = FakeContacts()
        val vm = ready(repo, alice)
        vm.updateCode("invalid")
        vm.preview()
        assertEquals(0, repo.previews)
        vm.updateCode(invite.code)
        vm.preview()
        repo.previewResult!!(Result.success(invite))
        assertNull(vm.state.preview)
        vm.respond(true)
        assertEquals(0, repo.responses)
        vm.bind(bob)
        repo.prepared.last()(Result.success(Unit)); repo.observers.last()(Result.success(emptyList()))
        vm.updateCode(invite.code); vm.preview()
        repo.previewResult!!(Result.success(invite.copy(expiresAtMillis = 0)))
        assertNull(vm.state.preview)
    }

    @Test fun previewDoesNotAcceptUntilExplicitConfirmationAndDoubleTapsAreIgnored() {
        val repo = FakeContacts()
        val vm = ready(repo, bob)
        vm.updateCode(" ${invite.code.uppercase()} ")
        vm.preview()
        repo.previewResult!!(Result.success(invite))
        assertEquals(invite, vm.state.preview)
        assertEquals(0, repo.responses)
        vm.respond(true); vm.respond(true)
        assertEquals(1, repo.responses)
        assertTrue(repo.accepted)
        repo.result!!(Result.success(Unit))
        assertNull(vm.state.preview)
        assertFalse(vm.state.busy)
    }

    @Test fun declineIsAnExplicitSeparateAction() {
        val repo = FakeContacts()
        val vm = ready(repo, bob)
        vm.updateCode(invite.code); vm.preview()
        repo.previewResult!!(Result.success(invite))
        vm.respond(false)
        assertFalse(repo.accepted)
        repo.result!!(Result.success(Unit))
        assertEquals("Invitation declined.", vm.state.status)
    }

    @Test fun lateMutationResultCannotAffectAnotherAccount() {
        val repo = FakeContacts()
        val vm = ready(repo, alice)
        vm.create()
        vm.bind(bob)
        repo.created!!(Result.success(invite))
        assertEquals(bob, vm.state.user)
        assertTrue(vm.state.busy)
        assertTrue(vm.state.invitations.isEmpty())
        assertEquals("Connecting to app contacts…", vm.state.status)
    }

    @Test fun revocationMustReferToAnOwnedOrReceivedConnection() {
        val repo = FakeContacts()
        val vm = ready(repo, bob)
        repo.observers.last()(Result.success(listOf(invite)))
        vm.revoke(invite.code)
        assertEquals(0, repo.revocations)
        repo.observers.last()(Result.success(listOf(invite.copy(toUid = "bob", toName = "Bob", status = InvitationStatus.ACCEPTED))))
        vm.revoke(invite.code)
        assertEquals(1, repo.revocations)
    }

    @Test fun existingConnectionCannotBeAddedAgainThroughTheUi() {
        val repo = FakeContacts()
        val vm = ready(repo, bob)
        repo.observers.last()(Result.success(listOf(invite.copy(toUid = "bob", status = InvitationStatus.ACCEPTED))))
        vm.updateCode(invite.code); vm.preview(); repo.previewResult!!(Result.success(invite))
        assertNull(vm.state.preview)
        assertEquals("You already support this person.", vm.state.status)
    }

    @Test fun createdCodeIsAvailableImmediatelyWithoutWaitingForTheList() {
        val repo = FakeContacts()
        val vm = ready(repo, alice)
        vm.create()
        repo.created!!(Result.success(invite))
        assertEquals(invite, vm.state.createdInvitation)
        assertTrue(vm.state.invitations.isEmpty())
        assertFalse(vm.state.busy)
        vm.dismissCreatedInvitation()
        assertNull(vm.state.createdInvitation)
    }

    @Test fun failedCreationDoesNotShowSuccessDialog() {
        val repo = FakeContacts()
        val vm = ready(repo, alice)
        vm.create()
        repo.created!!(Result.failure(Exception("Try again")))
        assertNull(vm.state.createdInvitation)
        assertEquals("Try again", vm.state.status)
        assertFalse(vm.state.busy)
    }

    @Test fun leavingPageClearsCodeAndLateCreationCannotReopenDialog() {
        val repo = FakeContacts()
        val vm = ready(repo, alice)
        vm.create()
        repo.created!!(Result.success(invite))
        vm.bind(null)
        assertNull(vm.state.createdInvitation)
        repo.created!!(Result.success(invite))
        assertNull(vm.state.createdInvitation)
    }

    private fun ready(repo: FakeContacts, user: AccountUser): AppContactsViewModel {
        val vm = AppContactsViewModel(repo)
        vm.bind(user)
        repo.prepared.last()(Result.success(Unit))
        repo.observers.last()(Result.success(emptyList()))
        return vm
    }

    private class FakeContacts : AppContactsRepository {
        val prepared = mutableListOf<(Result<Unit>) -> Unit>()
        val observers = mutableListOf<(Result<List<ContactInvitation>>) -> Unit>()
        var cancelled = false
        var creates = 0
        var previews = 0
        var responses = 0
        var revocations = 0
        var accepted = false
        var created: ((Result<ContactInvitation>) -> Unit)? = null
        var previewResult: ((Result<ContactInvitation>) -> Unit)? = null
        var result: ((Result<Unit>) -> Unit)? = null
        override fun prepare(user: AccountUser, done: (Result<Unit>) -> Unit) { prepared.add(done) }
        override fun observe(uid: String, changed: (Result<List<ContactInvitation>>) -> Unit): AccountSubscription {
            observers.add(changed); return AccountSubscription { cancelled = true }
        }
        override fun create(user: AccountUser, done: (Result<ContactInvitation>) -> Unit) { creates++; created = done }
        override fun preview(code: String, done: (Result<ContactInvitation>) -> Unit) { previews++; previewResult = done }
        override fun respond(user: AccountUser, code: String, accept: Boolean, done: (Result<Unit>) -> Unit) {
            responses++; accepted = accept; result = done
        }
        override fun revoke(uid: String, code: String, done: (Result<Unit>) -> Unit) { revocations++; result = done }
    }
}
