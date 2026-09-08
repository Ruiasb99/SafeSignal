package com.example.emergencybutton.data

import android.content.Context
import com.example.emergencybutton.EmergencyStorage
import com.example.emergencybutton.PinManager
import com.example.emergencybutton.ProtectionMode
import com.example.emergencybutton.SavedLocation
import com.example.emergencybutton.domain.EmergencyRepository
import com.example.emergencybutton.domain.PinRepository

// Keep the existing preferences and keys so updates preserve contacts, PINs and location.
class LocalEmergencyRepository(context: Context) : EmergencyRepository {
    private val context = context.applicationContext
    override fun loadContacts() = EmergencyStorage.loadContacts(context)
    override fun saveContacts(contacts: List<String>) = EmergencyStorage.saveContacts(context, contacts)
    override fun loadMode() = EmergencyStorage.loadMode(context)
    override fun saveMode(mode: ProtectionMode) = EmergencyStorage.saveMode(context, mode)
    override fun loadLocation() = EmergencyStorage.loadLocation(context)
    override fun saveLocation(location: SavedLocation) = EmergencyStorage.saveLocation(context, location)
    override fun isIncidentActive() = EmergencyStorage.isIncidentActive(context)
    override fun setIncidentActive(active: Boolean) = EmergencyStorage.setIncidentActive(context, active)
    override fun beginIncident(recipients: List<String>) = EmergencyStorage.beginIncident(context, recipients)
    override fun loadIncidentRecipients() = EmergencyStorage.loadIncidentRecipients(context)
}

class LocalPinRepository(context: Context) : PinRepository {
    private val context = context.applicationContext
    override fun hasPin() = PinManager.hasPin(context)
    override fun verifyPin(pin: String) = PinManager.verifyPin(context, pin)
    override fun savePin(pin: String) = PinManager.savePin(context, pin)
}
