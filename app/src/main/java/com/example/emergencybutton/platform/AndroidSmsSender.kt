package com.example.emergencybutton.platform

import android.telephony.SmsManager
import com.example.emergencybutton.domain.SmsSender
import com.example.emergencybutton.domain.SmsSubmission

class AndroidSmsSender : SmsSender {
    @Suppress("DEPRECATION")
    override fun send(recipients: List<String>, message: String): SmsSubmission {
        var submitted = 0
        // Submission is not proof of delivery; delivery callbacks remain in the backlog.
        recipients.forEach { recipient ->
            try {
                val manager = SmsManager.getDefault()
                manager.sendMultipartTextMessage(recipient, null, manager.divideMessage(message), null, null)
                submitted++
            } catch (_: Exception) {
                // A failure for one contact must not prevent attempts for the others.
            }
        }
        return SmsSubmission(submitted, recipients.size)
    }
}
