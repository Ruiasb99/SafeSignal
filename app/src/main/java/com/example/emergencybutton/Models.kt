package com.example.emergencybutton

enum class ProtectionMode(val intervalMinutes: Long?) {
    ARMED(15L),
    LOW_POWER(120L),
    OFF(null)
}

data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long
)
