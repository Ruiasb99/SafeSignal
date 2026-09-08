package com.example.emergencybutton.domain

import com.example.emergencybutton.ProtectionMode
import com.example.emergencybutton.SavedLocation

interface EmergencyRepository {
    fun loadContacts(): List<String>
    fun saveContacts(contacts: List<String>)
    fun loadMode(): ProtectionMode
    fun saveMode(mode: ProtectionMode)
    fun loadLocation(): SavedLocation?
    fun saveLocation(location: SavedLocation)
    fun isIncidentActive(): Boolean
    fun setIncidentActive(active: Boolean)
    /** Store the active flag and recipient snapshot together. */
    fun beginIncident(recipients: List<String>)
    fun loadIncidentRecipients(): List<String>
}

interface PinRepository {
    fun hasPin(): Boolean
    fun verifyPin(pin: String): Boolean
    fun savePin(pin: String)
}

fun interface CancelLocationRequest {
    fun cancel()
}

interface LocationProvider {
    /** Delivers at most one result on the main thread; cancellation suppresses delivery. */
    fun findLocation(onResult: (SavedLocation?) -> Unit): CancelLocationRequest
}

fun interface LocationScheduler {
    fun schedule(mode: ProtectionMode)
}

fun interface SmsSender {
    fun send(recipients: List<String>, message: String): SmsSubmission
}

data class SmsSubmission(val submitted: Int, val total: Int) {
    val status: String
        get() = when {
            total > 0 && submitted == total -> "SMS submitted for all $submitted contacts"
            submitted > 0 -> "SMS submitted for $submitted of $total contacts"
            else -> "SMS failed for every contact"
        }
}
