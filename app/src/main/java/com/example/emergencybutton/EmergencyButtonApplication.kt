package com.example.emergencybutton

import android.app.Application

class EmergencyButtonApplication : Application() {
    // One container per process keeps Firestore configuration stable across Activity recreation.
    val container by lazy { AppContainer(this) }
}
