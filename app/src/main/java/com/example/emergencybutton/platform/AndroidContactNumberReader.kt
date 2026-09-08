package com.example.emergencybutton.platform

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

/** Reads only the number selected through ACTION_PICK's temporary URI grant. */
class AndroidContactNumberReader(private val context: Context) {
    fun read(uri: Uri): String? {
        if (uri.scheme != "content") return null
        return context.contentResolver.query(uri, arrayOf(Phone.NUMBER, Phone.NORMALIZED_NUMBER),
            null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val normalized = cursor.getString(cursor.getColumnIndexOrThrow(Phone.NORMALIZED_NUMBER))
            if (!normalized.isNullOrBlank() && normalized.startsWith("+")) return@use normalized
            val raw = cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)) ?: return@use null
            // Prefer the home SIM's country over a roaming network. The user still reviews the draft.
            val phone = context.getSystemService(TelephonyManager::class.java)
            val country = phone?.simCountryIso?.takeIf { it.isNotBlank() }
                ?: phone?.networkCountryIso?.takeIf { it.isNotBlank() }
                ?: Locale.getDefault().country
            PhoneNumberUtils.formatNumberToE164(raw, country.uppercase(Locale.ROOT)) ?: raw
        }
    }
}
