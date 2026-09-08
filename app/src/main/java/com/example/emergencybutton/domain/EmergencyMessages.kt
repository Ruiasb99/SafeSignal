package com.example.emergencybutton.domain

import com.example.emergencybutton.SavedLocation

fun locationAge(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val minutes = (nowMillis - timestampMillis).coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1 -> "less than 1 minute old"
        minutes < 60 -> "$minutes minutes old"
        else -> "${minutes / 60} hours old"
    }
}

object EmergencyMessages {
    const val CANCELLATION = "EMERGENCY CANCELLED: The alert was cancelled by the user."

    fun initial(location: SavedLocation?, nowMillis: Long = System.currentTimeMillis()): String =
        if (location == null) {
            "EMERGENCY: I need help. Retrieving my current location."
        } else {
            "EMERGENCY: I need help. Last saved location (" +
                locationAge(location.timestampMillis, nowMillis) + "): " +
                mapsLink(location) + ". Retrieving my current location."
        }

    fun locationUpdate(location: SavedLocation): String = "LOCATION UPDATE: " + mapsLink(location)

    private fun mapsLink(location: SavedLocation): String =
        "https://maps.google.com/?q=${location.latitude},${location.longitude}"
}
