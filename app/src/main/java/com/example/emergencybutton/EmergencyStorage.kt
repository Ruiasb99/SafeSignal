package com.example.emergencybutton

import android.content.Context
import android.location.Location

object EmergencyStorage {
    private const val PREFERENCES_NAME = "emergency_button_preferences"
    private const val CONTACTS_KEY = "emergency_contacts"
    private const val MODE_KEY = "protection_mode"
    private const val LATITUDE_KEY = "last_latitude"
    private const val LONGITUDE_KEY = "last_longitude"
    private const val ACCURACY_KEY = "last_accuracy"
    private const val LOCATION_TIME_KEY = "last_location_time"
    private const val INCIDENT_ACTIVE_KEY = "incident_active"
    private const val INCIDENT_RECIPIENTS_KEY = "incident_recipients"

    fun loadContacts(context: Context): List<String> =
        preferences(context)
            .getStringSet(CONTACTS_KEY, emptySet())
            .orEmpty()
            .sorted()

    fun saveContacts(context: Context, contacts: List<String>) {
        preferences(context)
            .edit()
            .putStringSet(CONTACTS_KEY, contacts.toSet())
            .apply()
    }

    fun loadMode(context: Context): ProtectionMode {
        val saved = preferences(context).getString(MODE_KEY, ProtectionMode.OFF.name)
        return runCatching { ProtectionMode.valueOf(saved.orEmpty()) }
            .getOrDefault(ProtectionMode.OFF)
    }

    fun saveMode(context: Context, mode: ProtectionMode) {
        preferences(context).edit().putString(MODE_KEY, mode.name).apply()
    }

    fun saveLocation(context: Context, location: Location) {
        saveLocation(context, SavedLocation(
            location.latitude, location.longitude, location.accuracy, location.time
        ))
    }

    fun saveLocation(context: Context, location: SavedLocation) {
        preferences(context)
            .edit()
            .putLong(LATITUDE_KEY, location.latitude.toBits())
            .putLong(LONGITUDE_KEY, location.longitude.toBits())
            .putFloat(ACCURACY_KEY, location.accuracyMeters)
            .putLong(LOCATION_TIME_KEY, location.timestampMillis)
            .apply()
    }

    fun loadLocation(context: Context): SavedLocation? {
        val prefs = preferences(context)
        if (!prefs.contains(LOCATION_TIME_KEY)) return null

        return SavedLocation(
            latitude = Double.fromBits(prefs.getLong(LATITUDE_KEY, 0L)),
            longitude = Double.fromBits(prefs.getLong(LONGITUDE_KEY, 0L)),
            accuracyMeters = prefs.getFloat(ACCURACY_KEY, 0f),
            timestampMillis = prefs.getLong(LOCATION_TIME_KEY, 0L)
        )
    }

    fun isIncidentActive(context: Context): Boolean =
        preferences(context).getBoolean(INCIDENT_ACTIVE_KEY, false)

    fun beginIncident(context: Context, recipients: List<String>) {
        preferences(context).edit()
            .putStringSet(INCIDENT_RECIPIENTS_KEY, recipients.toSet())
            .putBoolean(INCIDENT_ACTIVE_KEY, true)
            .apply()
    }

    fun loadIncidentRecipients(context: Context): List<String> =
        preferences(context).getStringSet(INCIDENT_RECIPIENTS_KEY, null)?.sorted()
            ?: loadContacts(context) // Compatibility with incidents created before recipient snapshots.

    fun setIncidentActive(context: Context, active: Boolean) {
        preferences(context).edit().putBoolean(INCIDENT_ACTIVE_KEY, active).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
