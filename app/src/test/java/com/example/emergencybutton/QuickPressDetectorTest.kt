package com.example.emergencybutton

import com.example.emergencybutton.domain.ButtonEventDecoder
import com.example.emergencybutton.domain.QuickPressDetector
import org.junit.Assert.*
import org.junit.Test

class QuickPressDetectorTest {
    @Test fun triggersOnThirdNotFirstOrSecond() {
        val pattern = QuickPressDetector()
        assertFalse(pattern.press(0)); assertFalse(pattern.press(300)); assertTrue(pattern.press(600))
    }
    @Test fun exactWindowBoundaryCounts() {
        val pattern = QuickPressDetector()
        assertFalse(pattern.press(0)); assertFalse(pattern.press(1_000)); assertTrue(pattern.press(2_000))
    }
    @Test fun slowPressesExpireButLaterQuickTripleWorks() {
        val pattern = QuickPressDetector()
        assertFalse(pattern.press(0)); assertFalse(pattern.press(1_500)); assertFalse(pattern.press(2_001))
        assertTrue(pattern.press(2_300))
    }
    @Test fun bounceDoesNotCount() {
        val pattern = QuickPressDetector()
        assertFalse(pattern.press(0)); assertFalse(pattern.press(20)); assertFalse(pattern.press(40))
        assertFalse(pattern.press(200)); assertTrue(pattern.press(400))
    }
    @Test fun disconnectOrCancellationDiscardsPartialPattern() {
        val pattern = QuickPressDetector()
        pattern.press(0); pattern.press(200); pattern.reset()
        assertFalse(pattern.press(400)); assertFalse(pattern.press(600)); assertTrue(pattern.press(800))
    }
    @Test fun legacyIdleSosRequiresReleaseAndIgnoresHeldDuplicates() {
        val decoder = ButtonEventDecoder()
        decoder.baseline("IDLE")
        assertTrue(decoder.notification("SOS")); assertFalse(decoder.notification("SOS"))
        assertFalse(decoder.notification("IDLE")); assertTrue(decoder.notification("SOS"))
    }
    @Test fun readingOldSosDoesNotCountAndNeedsRelease() {
        val decoder = ButtonEventDecoder()
        decoder.baseline("SOS")
        assertFalse(decoder.notification("SOS"))
        decoder.notification("Idle")
        assertTrue(decoder.notification("SOS"))
    }
    @Test fun counterReadAndDuplicateOrOutOfOrderNotificationsNeverCount() {
        val decoder = ButtonEventDecoder()
        decoder.baseline("PRESS:7")
        assertFalse(decoder.notification("PRESS:7")); assertFalse(decoder.notification("PRESS:6"))
        assertTrue(decoder.notification("PRESS:8")); assertFalse(decoder.notification("PRESS:8"))
    }
    @Test fun missedSequenceIsOnlyOneObservedPressNotAnInventedTriple() {
        val decoder = ButtonEventDecoder()
        decoder.baseline("PRESS:0")
        val pattern = QuickPressDetector()
        assertTrue(decoder.notification("PRESS:3"))
        assertFalse(pattern.press(100))
    }
    @Test fun malformedUnknownAndOversizedCountersIgnored() {
        val decoder = ButtonEventDecoder()
        listOf("", "PRESS:-1", "PRESS:4294967296", "PRESS:abc", "Help", "PRESS:1:2").forEach {
            assertFalse(it, decoder.notification(it))
        }
    }
    @Test fun newConnectionCanEstablishNewCounterBaseline() {
        val decoder = ButtonEventDecoder()
        decoder.baseline("PRESS:999"); decoder.reset(); decoder.baseline("PRESS:0")
        assertTrue(decoder.notification("PRESS:1"))
    }
}
