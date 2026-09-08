package com.example.emergencybutton.domain

import com.example.emergencybutton.SavedLocation

data class IncidentState(val active: Boolean, val location: SavedLocation?, val status: String = "Ready")

/** Process-scoped, main-thread coordinator shared by the UI and the BLE service. No Activity lifetime. */
class EmergencyCoordinator(
    private val repository: EmergencyRepository,
    private val sms: SmsSender,
    private val location: LocationProvider
) {
    var state = IncidentState(repository.isIncidentActive(), repository.loadLocation())
        private set
    private var request: CancelLocationRequest? = null
    private var generation = 0L
    private val observers = mutableSetOf<(IncidentState) -> Unit>()

    fun observe(observer: (IncidentState) -> Unit): () -> Unit {
        observers.add(observer)
        observer(state)
        return { observers.remove(observer) }
    }

    fun start(recipients: List<String>, replaceActive: Boolean = false): Boolean {
        if (recipients.isEmpty() || (state.active && !replaceActive)) return false
        close()
        val token = generation
        val snapshot = recipients.toList()
        repository.beginIncident(snapshot)
        publish(state.copy(active = true))
        val result = sms.send(snapshot, EmergencyMessages.initial(repository.loadLocation()))
        publish(state.copy(status = "${result.status}; retrieving current location..."))
        request = location.findLocation { found ->
            if (token != generation || !state.active) return@findLocation
            if (found == null) publish(state.copy(status = "${result.status}; current location unavailable"))
            else {
                repository.saveLocation(found)
                publish(state.copy(location = found,
                    status = sms.send(snapshot, EmergencyMessages.locationUpdate(found)).status))
            }
        }
        return true
    }

    /** Caller must verify the cancellation PIN before invoking this. */
    fun cancel() {
        if (!state.active) return
        close()
        repository.setIncidentActive(false)
        val result = sms.send(repository.loadIncidentRecipients(), EmergencyMessages.CANCELLATION)
        publish(state.copy(active = false, status = "Emergency cancelled; ${result.status}"))
    }

    fun close() { generation++; request?.cancel(); request = null }
    private fun publish(value: IncidentState) {
        state = value
        observers.toList().forEach { it(value) }
    }
}
