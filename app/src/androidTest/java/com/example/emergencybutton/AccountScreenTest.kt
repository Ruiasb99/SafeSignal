package com.example.emergencybutton

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.emergencybutton.domain.AccountUser
import com.example.emergencybutton.ui.account.AccountForm
import com.example.emergencybutton.ui.account.AccountScreen
import com.example.emergencybutton.ui.account.AccountUiState
import com.example.emergencybutton.ui.theme.EmergencyButtonTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-only tests: no Firebase requests, SMS, or changes to saved contacts. */
@RunWith(AndroidJUnit4::class)
class AccountScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unconfiguredAccountExplainsAvailabilityAndDisablesSignIn() {
        show(AccountUiState(configured = false))
        compose.onNodeWithText("Accounts are not connected yet").assertIsDisplayed()
        compose.onNodeWithText("Sign in").assertIsNotEnabled()
        compose.onNodeWithText("New here? Create an account").performScrollTo().performClick()
        compose.onNodeWithText("Confirm password").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Create account").assertIsNotEnabled()
    }

    @Test fun passwordResetFormHasResetActionAndBackNavigation() {
        var backCount = 0
        show(AccountUiState(configured = true, form = AccountForm.RESET_PASSWORD)) { backCount++ }
        compose.onNodeWithText("Reset your password").assertIsDisplayed()
        compose.onNodeWithText("Send reset link").assertIsDisplayed()
        compose.onNodeWithText("Back to settings").assertIsDisplayed().performClick()
        assertEquals(1, backCount)
    }

    @Test fun signedInAccountShowsVerificationAndProfile() {
        show(AccountUiState(configured = true,
            user = AccountUser("test-only", "test@example.com", "Test Person", false), nameDraft = "Test Person"))
        compose.onNodeWithText("Email not verified yet").assertIsDisplayed()
        compose.onNodeWithText("Send verification email").assertIsDisplayed()
        compose.onNodeWithText("Save name").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sign out").performScrollTo().assertIsDisplayed()
    }

    private fun show(initial: AccountUiState, onBack: () -> Unit = {}) {
        val state = mutableStateOf(initial)
        compose.setContent {
            EmergencyButtonTheme(dynamicColor = false) {
                com.example.emergencybutton.ui.components.PageFrame("Back to settings", onBack) {
                AccountScreen(
                    state = state.value,
                    onEmailChange = {}, onPasswordChange = {}, onConfirmationChange = {}, onNameChange = {},
                    onFormChange = { state.value = state.value.copy(form = it) },
                    onSubmit = {}, onSaveName = {}, onSendVerification = {}, onRefresh = {},
                    onResetPassword = {}, onSignOut = {}, onBack = onBack
                )
                }
            }
        }
    }
}
