package com.example.emergencybutton.ui

import com.example.emergencybutton.ProtectionMode
import com.example.emergencybutton.SavedLocation

enum class AppScreen { DASHBOARD, CONTACTS, SETTINGS, ACCOUNT, APP_CONTACTS, BUTTON }

data class EmergencyUiState(
    val status: String = "Ready",
    val screen: AppScreen = AppScreen.DASHBOARD,
    val contacts: List<String> = emptyList(),
    val contactDraft: String = "",
    val editingContact: String? = null,
    val protectionMode: ProtectionMode = ProtectionMode.OFF,
    val savedLocation: SavedLocation? = null,
    val incidentActive: Boolean = false,
    val hasCancellationPin: Boolean = false,
    val currentPinDraft: String = "",
    val newPinDraft: String = "",
    val confirmPinDraft: String = "",
    val cancellationPinDraft: String = ""
)

enum class PermissionAction { NONE, EMERGENCY, MODE_LOCATION, BACKGROUND_SETTINGS }
