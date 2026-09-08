package com.example.emergencybutton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.emergencybutton.ui.components.PageFrame
import com.example.emergencybutton.ui.screens.ContactManagementScreen
import com.example.emergencybutton.ui.screens.EmergencyScreen
import com.example.emergencybutton.ui.theme.EmergencyButtonTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Fake callbacks only: these layout tests never send SMS or access real contacts. */
class NavigationLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sosLabelFitsAtLargerTextSize() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.3f)) {
                EmergencyButtonTheme(darkTheme = false, dynamicColor = false) {
                    EmergencyScreen(listOf("+49111111111"), ProtectionMode.OFF, null,
                        false, true, "", "Ready", {}, {}, {}, {}, "Button monitoring is off", {}, {}, {})
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("TAP FOR HELP").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertFalse(results.single().hasVisualOverflow)
    }

    @Test fun backStaysVisibleWithLongContactListOnNarrowScreen() {
        var backs = 0
        compose.setContent {
            EmergencyButtonTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    PageFrame("Back to dashboard", { backs++ }) {
                        ContactManagementScreen((1..20).map { "+491111111%02d".format(it) },
                            "", null, "Ready", {}, {}, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Back to dashboard").assertIsDisplayed()
        compose.onNodeWithText("Choose from phone contacts").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Back to dashboard").assertIsDisplayed().performClick()
        assertEquals(1, backs)
    }
}
