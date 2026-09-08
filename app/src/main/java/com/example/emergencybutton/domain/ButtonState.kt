package com.example.emergencybutton.domain

data class ButtonDevice(val address: String, val name: String)

/** UI-readable snapshot; contains no Bluetooth framework objects. */
data class BleState(
    val status: String = "Button monitoring is off",
    val monitoring: Boolean = false,
    val scanning: Boolean = false,
    val ready: Boolean = false,
    val devices: List<ButtonDevice> = emptyList(),
    val savedAddress: String? = null,
    val characteristic: String? = null,
    val receivedPresses: Int = 0
)
