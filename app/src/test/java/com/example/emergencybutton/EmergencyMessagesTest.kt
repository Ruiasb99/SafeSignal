package com.example.emergencybutton

import com.example.emergencybutton.domain.EmergencyMessages
import com.example.emergencybutton.domain.SmsSubmission
import com.example.emergencybutton.domain.locationAge
import org.junit.Assert.*
import org.junit.Test

class EmergencyMessagesTest {
    @Test fun initialAlertLabelsCachedCoordinatesWithAge() {
        val location = SavedLocation(52.52, 13.405, 10f, 60_000)
        assertEquals("EMERGENCY: I need help. Last saved location (2 minutes old): " +
            "https://maps.google.com/?q=52.52,13.405. Retrieving my current location.",
            EmergencyMessages.initial(location, 180_000))
    }

    @Test fun noCachedLocationStillProducesAnEmergencyMessage() {
        assertEquals("EMERGENCY: I need help. Retrieving my current location.",
            EmergencyMessages.initial(null))
    }

    @Test fun locationAgeHandlesClockChangesAndHourBoundary() {
        assertEquals("less than 1 minute old", locationAge(200_000, 100_000))
        assertEquals("59 minutes old", locationAge(0, 3_599_999))
        assertEquals("1 hours old", locationAge(0, 3_600_000))
    }

    @Test fun submissionStatusDistinguishesPartialFailureFromFullSubmission() {
        assertEquals("SMS submitted for 1 of 2 contacts", SmsSubmission(1, 2).status)
        assertEquals("SMS submitted for all 2 contacts", SmsSubmission(2, 2).status)
        assertEquals("SMS failed for every contact", SmsSubmission(0, 2).status)
    }
}
