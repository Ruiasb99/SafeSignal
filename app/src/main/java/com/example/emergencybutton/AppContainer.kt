package com.example.emergencybutton

import android.content.Context
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.emergencybutton.data.LocalEmergencyRepository
import com.example.emergencybutton.data.LocalPinRepository
import com.example.emergencybutton.platform.AndroidLocationProvider
import com.example.emergencybutton.platform.AndroidSmsSender
import com.example.emergencybutton.platform.WorkManagerLocationScheduler
import com.example.emergencybutton.ui.EmergencyViewModel
import com.example.emergencybutton.ui.account.AccountViewModel
import com.example.emergencybutton.data.FirebaseAccountRepository
import com.example.emergencybutton.data.UnavailableAccountRepository
import com.example.emergencybutton.domain.AccountRepository
import com.example.emergencybutton.domain.EmergencyCoordinator
import com.example.emergencybutton.domain.EmergencyRepository
import com.example.emergencybutton.domain.PinRepository
import com.example.emergencybutton.platform.ble.BleManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.example.emergencybutton.data.FirestoreAppContactsRepository
import com.example.emergencybutton.data.UnavailableAppContactsRepository
import com.example.emergencybutton.domain.AppContactsRepository
import com.example.emergencybutton.ui.contacts.AppContactsViewModel

/** Small composition root; tests supply fakes directly to the ViewModel. */
class AppContainer(context: Context) : ViewModelProvider.Factory {
    private val context = context.applicationContext
    val emergencyRepository: EmergencyRepository = LocalEmergencyRepository(this.context)
    val pins: PinRepository = LocalPinRepository(this.context)
    val emergencies = EmergencyCoordinator(
        emergencyRepository, AndroidSmsSender(), AndroidLocationProvider(this.context)
    )
    val ble by lazy { BleManager(this.context) }

    private val accounts: AccountRepository by lazy {
        val firebase = FirebaseApp.initializeApp(this.context)
        if (firebase == null) UnavailableAccountRepository()
        else FirebaseAccountRepository(FirebaseAuth.getInstance(firebase))
    }

    private val appContacts: AppContactsRepository by lazy {
        val firebase = FirebaseApp.initializeApp(this.context)
        if (firebase == null) UnavailableAppContactsRepository() else {
            val db = FirebaseFirestore.getInstance(firebase)
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
            FirestoreAppContactsRepository(db, FirebaseAuth.getInstance(firebase))
        }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            AppContactsViewModel::class.java -> AppContactsViewModel(appContacts)
            AccountViewModel::class.java -> AccountViewModel(accounts)
            EmergencyViewModel::class.java -> EmergencyViewModel(
                repository = emergencyRepository,
                pins = pins,
                scheduler = WorkManagerLocationScheduler(context),
                isPhoneNumber = PhoneNumberUtils::isGlobalPhoneNumber,
                coordinator = emergencies
            )
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}
