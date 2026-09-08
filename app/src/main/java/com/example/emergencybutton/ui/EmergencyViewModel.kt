package com.example.emergencybutton.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.emergencybutton.ProtectionMode
import com.example.emergencybutton.domain.EmergencyRepository
import com.example.emergencybutton.domain.LocationScheduler
import com.example.emergencybutton.domain.PinRepository
import com.example.emergencybutton.domain.EmergencyCoordinator

/** Owns screen state and user actions, with no Activity or Android service references. */
class EmergencyViewModel(
    private val repository: EmergencyRepository,
    private val pins: PinRepository,
    private val scheduler: LocationScheduler,
    private val isPhoneNumber: (String) -> Boolean,
    private val coordinator: EmergencyCoordinator
) : ViewModel() {
    var state by mutableStateOf(EmergencyUiState())
        private set

    private var pendingMode: ProtectionMode? = null
    private var pendingRecipients: List<String>? = null
    private val unsubscribe: () -> Unit

    init {
        val contacts = repository.loadContacts()
        state = EmergencyUiState(
            contacts = contacts,
            screen = if (contacts.isEmpty()) AppScreen.CONTACTS else AppScreen.DASHBOARD,
            protectionMode = repository.loadMode(),
            savedLocation = repository.loadLocation(),
            incidentActive = repository.isIncidentActive(),
            hasCancellationPin = pins.hasPin()
        )
        unsubscribe = coordinator.observe {
            state = state.copy(incidentActive = it.active, savedLocation = it.location, status = it.status)
        }
    }

    fun updateContactDraft(value: String) { state = state.copy(contactDraft = value) }
    fun updateCurrentPin(value: String) { state = state.copy(currentPinDraft = sanitizePin(value)) }
    fun updateNewPin(value: String) { state = state.copy(newPinDraft = sanitizePin(value)) }
    fun updateConfirmPin(value: String) { state = state.copy(confirmPinDraft = sanitizePin(value)) }
    fun updateCancellationPin(value: String) { state = state.copy(cancellationPinDraft = sanitizePin(value)) }
    fun openContacts() { state = state.copy(screen = AppScreen.CONTACTS) }
    fun openAppContacts() { state = state.copy(screen = AppScreen.APP_CONTACTS) }
    fun openSettings() { state = state.copy(screen = AppScreen.SETTINGS) }
    fun openButton() { state = state.copy(screen = AppScreen.BUTTON) }
    fun closeButton() { state = state.copy(screen = AppScreen.DASHBOARD) }
    fun openAccount() {
        state = state.copy(screen = AppScreen.ACCOUNT, currentPinDraft = "", newPinDraft = "", confirmPinDraft = "")
    }

    fun saveContact() {
        val number = state.contactDraft.trim().replace(" ", "")
        if (!isPhoneNumber(number)) {
            setStatus("Enter a valid phone number with country code")
            return
        }
        if (number in state.contacts && number != state.editingContact) {
            setStatus("That contact is already saved")
            return
        }
        val contacts = if (state.editingContact == null) state.contacts + number
            else state.contacts.map { if (it == state.editingContact) number else it }
        val saved = contacts.distinct().sorted()
        repository.saveContacts(saved)
        state = state.copy(contacts = saved, contactDraft = "", editingContact = null,
            status = "${saved.size} emergency contact(s) saved")
    }

    fun startEditingContact(number: String) {
        state = state.copy(editingContact = number, contactDraft = number, status = "Editing contact")
    }

    fun cancelContactEdit() {
        state = state.copy(editingContact = null, contactDraft = "", status = "Edit cancelled")
    }

    fun removeContact(number: String) {
        val contacts = state.contacts.filterNot { it == number }
        repository.saveContacts(contacts)
        if (state.editingContact == number) cancelContactEdit()
        state = state.copy(contacts = contacts, status = if (contacts.isEmpty())
            "Add an emergency contact to continue" else "Contact removed")
    }

    fun closeContacts() {
        if (state.contacts.isEmpty()) {
            setStatus("Save at least one emergency contact first")
        } else {
            state = state.copy(contactDraft = "", editingContact = null,
                screen = AppScreen.DASHBOARD, status = "Ready")
        }
    }

    fun saveCancellationPin() {
        if (state.hasCancellationPin && !pins.verifyPin(state.currentPinDraft)) {
            setStatus("Current PIN is incorrect")
            return
        }
        if (state.newPinDraft.length !in 4..6 || !state.newPinDraft.all(Char::isDigit)) {
            setStatus("Choose a PIN containing 4 to 6 digits")
            return
        }
        if (state.newPinDraft != state.confirmPinDraft) {
            setStatus("The new PINs do not match")
            return
        }
        pins.savePin(state.newPinDraft)
        state = state.copy(hasCancellationPin = true, currentPinDraft = "", newPinDraft = "",
            confirmPinDraft = "", status = "Cancellation PIN saved")
    }

    fun closeSettings() {
        state = state.copy(currentPinDraft = "", newPinDraft = "", confirmPinDraft = "",
            screen = if (state.contacts.isEmpty()) AppScreen.CONTACTS else AppScreen.DASHBOARD,
            status = "Ready")
    }

    fun cancelEmergency() {
        if (!state.incidentActive) return
        if (!state.hasCancellationPin) {
            state = state.copy(status = "Create a cancellation PIN first", screen = AppScreen.SETTINGS)
            return
        }
        if (!pins.verifyPin(state.cancellationPinDraft)) {
            setStatus("Incorrect cancellation PIN")
            return
        }
        pendingRecipients = null
        coordinator.cancel()
        state = state.copy(cancellationPinDraft = "")
    }

    fun selectMode(mode: ProtectionMode, foregroundGranted: Boolean, backgroundGranted: Boolean): PermissionAction {
        pendingMode = mode
        return when {
            mode == ProtectionMode.OFF -> { applyMode(mode); PermissionAction.NONE }
            !foregroundGranted -> PermissionAction.MODE_LOCATION
            !backgroundGranted -> backgroundSettingsAction()
            else -> { applyMode(mode); PermissionAction.NONE }
        }
    }

    fun onModePermissionResult(foregroundGranted: Boolean, backgroundGranted: Boolean): PermissionAction {
        val mode = pendingMode ?: return PermissionAction.NONE
        if (!foregroundGranted) {
            pendingMode = null
            setStatus("Location permission is required for scheduled updates")
            return PermissionAction.NONE
        }
        if (!backgroundGranted) return backgroundSettingsAction()
        applyMode(mode)
        return PermissionAction.NONE
    }

    fun onResume(backgroundGranted: Boolean) {
        state = state.copy(savedLocation = repository.loadLocation())
        pendingMode?.let { if (backgroundGranted) applyMode(it) }
    }

    private fun backgroundSettingsAction(): PermissionAction {
        setStatus("Set Location to Allow all the time, then return to the app")
        return PermissionAction.BACKGROUND_SETTINGS
    }

    private fun applyMode(mode: ProtectionMode) {
        pendingMode = null
        scheduler.schedule(mode)
        repository.saveMode(mode)
        state = state.copy(protectionMode = mode, status = when (mode) {
            ProtectionMode.OFF -> "Protection is off"
            ProtectionMode.ARMED -> "Armed: location updates about every 15 minutes"
            ProtectionMode.LOW_POWER -> "Low Power: location updates about every 2 hours"
        })
    }

    fun beginEmergency(permissionsGranted: Boolean): PermissionAction {
        if (state.contacts.isEmpty()) {
            setStatus("Add an emergency contact first")
            return PermissionAction.NONE
        }
        pendingRecipients = state.contacts.toList()
        if (permissionsGranted) {
            sendEmergency()
            return PermissionAction.NONE
        }
        setStatus("Waiting for permissions...")
        return PermissionAction.EMERGENCY
    }

    fun onEmergencyPermissionResult(granted: Boolean) {
        // A restored permission result must never start a new SOS without a pending action.
        if (pendingRecipients == null) return
        if (granted) sendEmergency() else {
            pendingRecipients = null
            setStatus("SMS and location permissions are required")
        }
    }

    private fun sendEmergency() {
        val recipients = pendingRecipients ?: return
        pendingRecipients = null
        coordinator.start(recipients, replaceActive = true)
    }

    private fun setStatus(value: String) { state = state.copy(status = value) }
    private fun sanitizePin(value: String) = value.filter(Char::isDigit).take(6)

    override fun onCleared() {
        unsubscribe()
        super.onCleared()
    }
}
