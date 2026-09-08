package com.example.emergencybutton.platform.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.emergencybutton.domain.BleState
import com.example.emergencybutton.domain.ButtonDevice
import com.example.emergencybutton.domain.ButtonEventDecoder
import com.example.emergencybutton.domain.QuickPressDetector
import java.util.UUID

/** All public methods/state changes are main-thread; callbacks are marshalled there. */
@SuppressLint("MissingPermission") // Activity/service gate grants; revocation is also caught.
class BleManager(context: Context) {
    private val context = context.applicationContext
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val prefs = context.getSharedPreferences("ble_button", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var scan: ScanCallback? = null
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var timeout: Runnable? = null
    private var retry: Runnable? = null
    private var retryDelay = 2_000L
    private val decoder = ButtonEventDecoder()
    private val detector = QuickPressDetector()
    var onTrigger: (() -> Unit)? = null
    var onStatus: (() -> Unit)? = null
    private val mutableStates = MutableStateFlow(BleState(savedAddress = prefs.getString("address", null)))
    val states = mutableStates.asStateFlow()
    val state: BleState get() = states.value

    fun message(text: String) { update(state.copy(status = text)) }
    private fun update(value: BleState) { mutableStates.value = value; onStatus?.invoke() }

    fun discover() {
        if (state.monitoring) return
        stopDiscovery()
        try {
            val activeScanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
                ?: return message("Turn on Bluetooth, then scan again")
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    handler.post {
                        if (scan !== this) return@post
                        try {
                            val found = ButtonDevice(result.device.address,
                                result.scanRecord?.deviceName ?: "Emergency Button")
                            update(state.copy(devices = (state.devices + found).distinctBy { it.address }))
                        } catch (_: SecurityException) { stopDiscovery(); message("Nearby devices permission was removed") }
                    }
                }
                override fun onScanFailed(errorCode: Int) {
                    handler.post { if (scan === this) { stopDiscovery(); message("Scan failed ($errorCode). Try again shortly") } }
                }
            }
            scan = callback
            scanner = activeScanner
            update(state.copy(scanning = true, devices = emptyList(), status = "Scanning for Emergency Button…"))
            activeScanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
            handler.postDelayed({
                if (scan === callback) {
                    stopDiscovery()
                    message(if (state.devices.isEmpty()) "No button found. Check power and service advertising" else "Select your button below")
                }
            }, 10_000L)
        } catch (_: SecurityException) { stopDiscovery(); message("Allow Nearby devices permission first") }
        catch (_: IllegalStateException) { stopDiscovery(); message("Bluetooth is unavailable; try again") }
    }

    fun stopDiscovery() {
        val callback = scan
        scan = null
        try { if (callback != null) scanner?.stopScan(callback) } catch (_: SecurityException) { }
        catch (_: IllegalStateException) { }
        scanner = null
        update(state.copy(scanning = false))
    }

    fun remember(address: String) {
        if (state.monitoring || !BluetoothAdapter.checkBluetoothAddress(address)) return
        prefs.edit().putString("address", address).apply()
        update(state.copy(savedAddress = address, status = "Button selected. Start monitoring when ready"))
        stopDiscovery()
    }

    fun forget() {
        if (state.monitoring) return
        prefs.edit().remove("address").apply()
        update(state.copy(savedAddress = null, characteristic = null, status = "Button forgotten"))
    }

    fun start() {
        if (state.monitoring) return
        if (state.savedAddress == null) return message("Select a button first")
        stopDiscovery()
        retryDelay = 2_000L
        update(state.copy(monitoring = true, receivedPresses = 0))
        connect()
    }

    fun stop() {
        retry?.let(handler::removeCallbacks); retry = null
        closeGatt()
        update(state.copy(monitoring = false, ready = false, status = "Button monitoring is off"))
    }

    fun resetPattern() { detector.reset() }

    private fun connect() {
        if (!state.monitoring) return
        closeGatt()
        try {
            val activeAdapter = adapter?.takeIf { it.isEnabled } ?: return failed("Bluetooth is off")
            val address = state.savedAddress ?: return stop()
            update(state.copy(ready = false, characteristic = null, status = "Connecting to your button…"))
            gatt = activeAdapter.getRemoteDevice(address).connectGatt(context, false, callbacks, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) { failed("Connection could not start"); return }
            armTimeout("Connection timed out")
        } catch (_: SecurityException) { failed("Nearby devices permission is missing", false) }
        catch (_: IllegalArgumentException) { failed("Invalid device address; forget and scan again", false) }
        catch (_: IllegalStateException) { failed("Bluetooth is unavailable") }
    }

    private fun armTimeout(reason: String) {
        timeout?.let(handler::removeCallbacks)
        timeout = Runnable { failed(reason) }.also { handler.postDelayed(it, 15_000L) }
    }

    private fun closeGatt() {
        timeout?.let(handler::removeCallbacks); timeout = null
        val previous = gatt
        gatt = null
        characteristic = null
        decoder.reset(); detector.reset()
        try { previous?.disconnect(); previous?.close() } catch (_: SecurityException) { }
        catch (_: IllegalStateException) { }
    }

    private fun failed(reason: String, retryAllowed: Boolean = true) {
        closeGatt()
        retry?.let(handler::removeCallbacks); retry = null
        update(state.copy(ready = false, status = if (state.monitoring && retryAllowed)
            "$reason. Reconnecting in ${retryDelay / 1000}s…" else "$reason. Stop monitoring to retry setup"))
        if (state.monitoring && retryAllowed) {
            retry = Runnable { retry = null; connect() }.also { handler.postDelayed(it, retryDelay) }
            retryDelay = (retryDelay * 2).coerceAtMost(60_000L)
        }
    }

    private fun current(connection: BluetoothGatt, block: () -> Unit) {
        handler.post {
            if (connection !== gatt || !state.monitoring) return@post
            try { block() } catch (_: SecurityException) { failed("Nearby devices permission was removed", false) }
            catch (_: IllegalStateException) { failed("Bluetooth operation failed") }
        }
    }

    private val callbacks = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, newState: Int) = current(connection) {
            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                failed("Button disconnected ($status)")
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                message("Connected; discovering button service…")
                armTimeout("Service discovery timed out")
                if (!connection.discoverServices()) failed("Could not discover services")
            }
        }

        override fun onServicesDiscovered(connection: BluetoothGatt, status: Int) = current(connection) {
            if (status != BluetoothGatt.GATT_SUCCESS) { failed("Service discovery failed ($status)"); return@current }
            val candidates = connection.getService(SERVICE_UUID)?.characteristics.orEmpty().filter {
                it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            }
            val selected = candidates.find { it.uuid == EVENT_UUID } ?: candidates.singleOrNull()
            if (selected == null) {
                failed("Firmware needs one notification characteristic in the Emergency Button service", false)
                return@current
            }
            characteristic = selected
            update(state.copy(characteristic = selected.uuid.toString(), status = "Preparing button notifications…"))
            armTimeout("Notification setup timed out")
            if (selected.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                if (!connection.readCharacteristic(selected)) failed("Could not read button baseline")
            } else subscribe(connection, selected)
        }

        @Deprecated("Legacy callback for Android 12")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(connection: BluetoothGatt, item: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < 33) read(connection, item, item.value?.copyOf() ?: byteArrayOf(), status)
        }
        override fun onCharacteristicRead(connection: BluetoothGatt, item: BluetoothGattCharacteristic, value: ByteArray, status: Int) =
            read(connection, item, value.copyOf(), status)

        override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) = current(connection) {
            if (descriptor.uuid != CCCD_UUID || descriptor.characteristic.uuid != characteristic?.uuid) return@current
            if (status != BluetoothGatt.GATT_SUCCESS) { failed("Button rejected notifications ($status)"); return@current }
            timeout?.let(handler::removeCallbacks); timeout = null
            retryDelay = 2_000L
            update(state.copy(ready = true, status = "Listening · 3 quick presses send SOS"))
        }

        @Deprecated("Legacy callback for Android 12")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(connection: BluetoothGatt, item: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) changed(connection, item, item.value?.copyOf() ?: byteArrayOf())
        }
        override fun onCharacteristicChanged(connection: BluetoothGatt, item: BluetoothGattCharacteristic, value: ByteArray) =
            changed(connection, item, value.copyOf())
    }

    private fun read(connection: BluetoothGatt, item: BluetoothGattCharacteristic, value: ByteArray, status: Int) = current(connection) {
        if (item.uuid != characteristic?.uuid || state.ready) return@current
        if (status != BluetoothGatt.GATT_SUCCESS) { failed("Could not read button baseline ($status)"); return@current }
        decoder.baseline(value.toString(Charsets.UTF_8))
        subscribe(connection, item)
    }

    private fun subscribe(connection: BluetoothGatt, item: BluetoothGattCharacteristic) {
        val descriptor = item.getDescriptor(CCCD_UUID)
        if (descriptor == null || !connection.setCharacteristicNotification(item, true)) {
            failed("Firmware lacks a usable notification descriptor", false); return
        }
        val value = if (item.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        val started = if (Build.VERSION.SDK_INT >= 33) connection.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            connection.writeDescriptor(descriptor)
        }
        if (!started) failed("Could not subscribe to button notifications")
    }

    private fun changed(connection: BluetoothGatt, item: BluetoothGattCharacteristic, value: ByteArray) = current(connection) {
        if (!state.ready || item.uuid != characteristic?.uuid || value.size > 32) return@current
        if (decoder.notification(value.toString(Charsets.UTF_8))) {
            update(state.copy(receivedPresses = state.receivedPresses + 1))
            if (detector.press(SystemClock.elapsedRealtime())) onTrigger?.invoke()
        }
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val EVENT_UUID: UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
