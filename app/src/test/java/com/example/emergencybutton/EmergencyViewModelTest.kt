package com.example.emergencybutton

import androidx.lifecycle.ViewModelStore
import com.example.emergencybutton.domain.*
import com.example.emergencybutton.ui.AppScreen
import com.example.emergencybutton.ui.EmergencyViewModel
import com.example.emergencybutton.ui.PermissionAction
import org.junit.Assert.*
import org.junit.Test

class EmergencyViewModelTest {
    private val first = "+49111111111"
    private val second = "+49222222222"
    private val fix = SavedLocation(52.52, 13.405, 12f, 1_000L)

    @Test fun firstLaunchRequiresContactAndLastRemovalBlocksDashboard() {
        val fixture = Fixture()
        val vm = fixture.vm
        assertEquals(AppScreen.CONTACTS, vm.state.screen)
        vm.closeContacts()
        assertEquals(AppScreen.CONTACTS, vm.state.screen)
        vm.updateContactDraft(" +49 111111111 ")
        vm.saveContact()
        assertEquals(listOf(first), fixture.repository.contacts)
        vm.closeContacts()
        assertEquals(AppScreen.DASHBOARD, vm.state.screen)
        vm.openContacts()
        vm.removeContact(first)
        vm.closeContacts()
        assertEquals(AppScreen.CONTACTS, vm.state.screen)
    }

    @Test fun contactsCanBeUpdatedButNotDuplicated() {
        val fixture = Fixture(listOf(first, second))
        val vm = fixture.vm
        vm.startEditingContact(first)
        vm.updateContactDraft(second)
        vm.saveContact()
        assertEquals(listOf(first, second), fixture.repository.contacts)
        assertEquals("That contact is already saved", vm.state.status)
        vm.updateContactDraft("+49333333333")
        vm.saveContact()
        assertEquals(listOf(second, "+49333333333"), fixture.repository.contacts)
        assertNull(vm.state.editingContact)
    }

    @Test fun invalidContactDoesNotPersist() {
        val fixture = Fixture()
        fixture.vm.updateContactDraft("invalid")
        fixture.vm.saveContact()
        assertTrue(fixture.repository.contacts.isEmpty())
    }

    @Test fun pinCreationRequiresMatchingConfirmationAndChangeRequiresCurrentPin() {
        val fixture = Fixture()
        val vm = fixture.vm
        vm.updateNewPin("1234")
        vm.updateConfirmPin("4321")
        vm.saveCancellationPin()
        assertFalse(vm.state.hasCancellationPin)
        vm.updateConfirmPin("1234")
        vm.saveCancellationPin()
        assertTrue(vm.state.hasCancellationPin)
        assertEquals("", vm.state.newPinDraft)
        vm.updateNewPin("5678")
        vm.updateConfirmPin("5678")
        vm.updateCurrentPin("0000")
        vm.saveCancellationPin()
        assertTrue(fixture.pins.verifyPin("1234"))
        vm.updateCurrentPin("1234")
        vm.saveCancellationPin()
        assertTrue(fixture.pins.verifyPin("5678"))
    }

    @Test fun closingSettingsClearsSensitiveDrafts() {
        val vm = Fixture().vm
        vm.openSettings()
        vm.updateCurrentPin("1234")
        vm.updateNewPin("5678")
        vm.updateConfirmPin("5678")
        vm.closeSettings()
        assertEquals("", vm.state.currentPinDraft)
        assertEquals("", vm.state.newPinDraft)
        assertEquals("", vm.state.confirmPinDraft)
        assertEquals(AppScreen.CONTACTS, vm.state.screen)
    }

    @Test fun sosSendsInitialThenLocationToTheSameRecipientSnapshot() {
        val fixture = Fixture(listOf(first, second))
        val vm = fixture.vm
        vm.beginEmergency(true)
        assertTrue(fixture.repository.active)
        assertEquals(listOf(first, second), fixture.sms.messages.single().recipients)
        assertTrue(fixture.sms.messages.single().text.startsWith("EMERGENCY:"))
        vm.removeContact(second)
        fixture.location.respond(0, fix)
        assertEquals(2, fixture.sms.messages.size)
        assertEquals(listOf(first, second), fixture.sms.messages.last().recipients)
        assertEquals("LOCATION UPDATE: https://maps.google.com/?q=52.52,13.405",
            fixture.sms.messages.last().text)
        assertEquals(fix, fixture.repository.location)
    }

    @Test fun cachedLocationIncludedInFirstAlertAndMissingFixDoesNotSendBogusCoordinates() {
        val fixture = Fixture(listOf(first))
        fixture.repository.location = fix
        fixture.vm.beginEmergency(true)
        assertTrue(fixture.sms.messages.single().text.contains("Last saved location"))
        assertTrue(fixture.sms.messages.single().text.contains("52.52,13.405"))
        fixture.location.respond(0, null)
        assertEquals(1, fixture.sms.messages.size)
        assertTrue(fixture.vm.state.status.contains("current location unavailable"))
        assertEquals(fix, fixture.repository.location)
    }

    @Test fun deniedPermissionsDoNotStartAnIncidentOrSendMessages() {
        val fixture = Fixture(listOf(first))
        assertEquals(PermissionAction.EMERGENCY, fixture.vm.beginEmergency(false))
        fixture.vm.onEmergencyPermissionResult(false)
        assertFalse(fixture.repository.active)
        assertTrue(fixture.sms.messages.isEmpty())
        fixture.vm.onEmergencyPermissionResult(true)
        assertTrue(fixture.sms.messages.isEmpty())
    }

    @Test fun grantingPermissionContinuesPendingSosOnlyOnce() {
        val fixture = Fixture(listOf(first))
        fixture.vm.beginEmergency(false)
        fixture.vm.onEmergencyPermissionResult(true)
        fixture.vm.onEmergencyPermissionResult(true)
        assertEquals(1, fixture.sms.messages.size)
        assertTrue(fixture.repository.active)
    }

    @Test fun wrongPinCannotCancelAndCorrectPinSuppressesLateLocation() {
        val fixture = Fixture(listOf(first, second), "1234")
        val vm = fixture.vm
        vm.beginEmergency(true)
        vm.updateCancellationPin("0000")
        vm.cancelEmergency()
        assertTrue(vm.state.incidentActive)
        assertEquals(1, fixture.sms.messages.size)
        vm.updateCancellationPin("1234")
        vm.cancelEmergency()
        assertFalse(vm.state.incidentActive)
        assertFalse(fixture.repository.active)
        assertTrue(fixture.location.requests[0].cancelled)
        assertEquals(listOf(first, second), fixture.sms.messages.last().recipients)
        assertEquals(EmergencyMessages.CANCELLATION, fixture.sms.messages.last().text)
        fixture.location.respond(0, fix) // Simulate an already queued callback after cancellation.
        assertEquals(2, fixture.sms.messages.size)
        assertNull(fixture.repository.location)
    }

    @Test fun replacingAnSosIgnoresOldLocationEvenWhileNewIncidentIsActive() {
        val fixture = Fixture(listOf(first))
        fixture.vm.beginEmergency(true)
        fixture.vm.beginEmergency(true)
        assertTrue(fixture.location.requests[0].cancelled)
        fixture.location.respond(0, fix)
        assertEquals(2, fixture.sms.messages.size)
        fixture.location.respond(1, fix)
        assertEquals(3, fixture.sms.messages.size)
    }

    @Test fun smsFailureIsNotReportedAsSuccessfulAlertOrCancellation() {
        val fixture = Fixture(listOf(first), "1234")
        fixture.sms.submitted = 0
        fixture.vm.beginEmergency(true)
        assertTrue(fixture.vm.state.status.contains("SMS failed"))
        fixture.vm.updateCancellationPin("1234")
        fixture.vm.cancelEmergency()
        assertTrue(fixture.vm.state.status.contains("SMS failed"))
        assertFalse(fixture.vm.state.status.contains("contacts notified"))
    }

    @Test fun modesWaitForPermissionsAndOffCancelsPendingModeWithoutCancellingSos() {
        val fixture = Fixture(listOf(first))
        val vm = fixture.vm
        assertEquals(PermissionAction.MODE_LOCATION, vm.selectMode(ProtectionMode.ARMED, false, false))
        assertEquals(PermissionAction.BACKGROUND_SETTINGS, vm.onModePermissionResult(true, false))
        vm.onResume(false)
        assertTrue(fixture.scheduled.isEmpty())
        vm.onResume(true)
        assertEquals(listOf(ProtectionMode.ARMED), fixture.scheduled)
        vm.selectMode(ProtectionMode.LOW_POWER, true, true)
        assertEquals(ProtectionMode.LOW_POWER, fixture.repository.mode)
        vm.selectMode(ProtectionMode.ARMED, true, false)
        vm.beginEmergency(true)
        vm.selectMode(ProtectionMode.OFF, false, false)
        vm.onResume(true)
        assertEquals(ProtectionMode.OFF, fixture.scheduled.last())
        assertTrue(vm.state.incidentActive)
        assertEquals(15L, ProtectionMode.ARMED.intervalMinutes)
        assertEquals(120L, ProtectionMode.LOW_POWER.intervalMinutes)
        assertNull(ProtectionMode.OFF.intervalMinutes)
    }

    @Test fun persistedStateRestoresWithoutResendingAnSos() {
        val fixture = Fixture(listOf(first), "1234")
        fixture.repository.active = true
        fixture.repository.mode = ProtectionMode.LOW_POWER
        fixture.repository.location = fix
        val restored = fixture.newViewModel()
        assertTrue(restored.state.incidentActive)
        assertTrue(restored.state.hasCancellationPin)
        assertEquals(ProtectionMode.LOW_POWER, restored.state.protectionMode)
        assertEquals(fix, restored.state.savedLocation)
        assertTrue(fixture.sms.messages.isEmpty())
    }

    @Test fun clearingScreenDetachesObserverButSharedLocationRequestCompletes() {
        val fixture = Fixture(listOf(first))
        val store = ViewModelStore()
        store.put("emergency", fixture.vm)
        fixture.vm.beginEmergency(true)
        store.clear()
        assertFalse(fixture.location.requests.single().cancelled)
        fixture.location.respond(0, fix)
        assertEquals(2, fixture.sms.messages.size)
        assertNull(fixture.vm.state.savedLocation)
    }

    @Test fun hardwareTriggersUseSharedCoordinatorWithoutAnOpenScreenAndDoNotRepeat() {
        val fixture = Fixture(listOf(first), "1234")
        val coordinator = EmergencyCoordinator(fixture.repository, fixture.sms, fixture.location)
        val vm = EmergencyViewModel(fixture.repository, fixture.pins,
            LocationScheduler { }, { true }, coordinator)
        assertTrue(coordinator.start(listOf(first)))
        repeat(5) { assertFalse(coordinator.start(listOf(first))) }
        assertTrue(vm.state.incidentActive)
        assertEquals(1, fixture.sms.messages.size)
        val store = ViewModelStore()
        store.put("screen", vm); store.clear()
        assertFalse(fixture.location.requests.single().cancelled)
        fixture.location.respond(0, fix)
        assertEquals(2, fixture.sms.messages.size)
    }

    @Test fun sharedHardwareSosCanBeCancelledByPinAndNewTripleCanStartAgain() {
        val fixture = Fixture(listOf(first), "1234")
        val coordinator = EmergencyCoordinator(fixture.repository, fixture.sms, fixture.location)
        val vm = EmergencyViewModel(fixture.repository, fixture.pins,
            LocationScheduler { }, { true }, coordinator)
        coordinator.start(listOf(first))
        vm.updateCancellationPin("9999"); vm.cancelEmergency()
        assertTrue(coordinator.state.active)
        vm.updateCancellationPin("1234"); vm.cancelEmergency()
        assertFalse(coordinator.state.active)
        fixture.location.respond(0, fix)
        assertEquals(2, fixture.sms.messages.size)
        assertTrue(coordinator.start(listOf(first)))
    }

    @Test fun cancellationUsesOriginalRecipientsAfterContactsAreEditedAndCoordinatorRestarts() {
        val fixture = Fixture(listOf(first, second), "1234")
        fixture.vm.beginEmergency(true)
        fixture.vm.removeContact(second)
        val restored = fixture.newViewModel()
        restored.updateCancellationPin("1234"); restored.cancelEmergency()
        assertEquals(listOf(first, second), fixture.sms.messages.last().recipients)
    }

    @Test fun numberedNotificationPipelineSendsExactlyOneSosForManyRapidPresses() {
        val fixture = Fixture(listOf(first, second))
        val coordinator = EmergencyCoordinator(fixture.repository, fixture.sms, fixture.location)
        val decoder = ButtonEventDecoder()
        val detector = QuickPressDetector()
        decoder.baseline("PRESS:0")
        for (index in 1..12) {
            if (decoder.notification("PRESS:$index") && detector.press(index * 150L)) {
                coordinator.start(fixture.repository.loadContacts())
            }
            assertFalse(decoder.notification("PRESS:$index"))
            assertEquals(if (index < 3) 0 else 1, fixture.sms.messages.size)
        }
        fixture.location.respond(0, fix)
        assertEquals(2, fixture.sms.messages.size)
        assertEquals(listOf(first, second), fixture.sms.messages.last().recipients)
    }

    private class Fixture(contacts: List<String> = emptyList(), pin: String? = null) {
        val repository = FakeRepository(contacts)
        val pins = FakePins(pin)
        val sms = FakeSms()
        val location = FakeLocation()
        val scheduled = mutableListOf<ProtectionMode>()
        val vm = newViewModel()
        fun newViewModel() = EmergencyViewModel(repository, pins,
            LocationScheduler { scheduled.add(it) }, { it.matches(Regex("\\+?[0-9]{7,15}")) },
            EmergencyCoordinator(repository, sms, location))
    }

    private class FakeRepository(var contacts: List<String>) : EmergencyRepository {
        var mode = ProtectionMode.OFF
        var location: SavedLocation? = null
        var active = false
        override fun loadContacts() = contacts
        override fun saveContacts(contacts: List<String>) { this.contacts = contacts }
        override fun loadMode() = mode
        override fun saveMode(mode: ProtectionMode) { this.mode = mode }
        override fun loadLocation() = location
        override fun saveLocation(location: SavedLocation) { this.location = location }
        override fun isIncidentActive() = active
        override fun setIncidentActive(active: Boolean) { this.active = active }
        var incidentRecipients = emptyList<String>()
        override fun beginIncident(recipients: List<String>) { incidentRecipients = recipients; active = true }
        override fun loadIncidentRecipients() = incidentRecipients.ifEmpty { contacts }
    }

    private class FakePins(var pin: String?) : PinRepository {
        override fun hasPin() = pin != null
        override fun verifyPin(pin: String) = this.pin == pin
        override fun savePin(pin: String) { this.pin = pin }
    }

    private data class Message(val recipients: List<String>, val text: String)
    private class FakeSms : SmsSender {
        val messages = mutableListOf<Message>()
        var submitted: Int? = null
        override fun send(recipients: List<String>, message: String): SmsSubmission {
            messages.add(Message(recipients.toList(), message))
            return SmsSubmission(submitted ?: recipients.size, recipients.size)
        }
    }

    private class FakeLocation : LocationProvider {
        class Request(val callback: (SavedLocation?) -> Unit, var cancelled: Boolean = false)
        val requests = mutableListOf<Request>()
        override fun findLocation(onResult: (SavedLocation?) -> Unit): CancelLocationRequest {
            val request = Request(onResult)
            requests.add(request)
            return CancelLocationRequest { request.cancelled = true }
        }
        fun respond(index: Int, location: SavedLocation?) { requests[index].callback(location) }
    }
}
