package com.example.emergencybutton.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.emergencybutton.domain.BleState
import com.example.emergencybutton.ui.screens.ButtonScreen
import com.example.emergencybutton.ui.contacts.AppContactsScreen
import com.example.emergencybutton.ui.contacts.AppContactsViewModel
import com.example.emergencybutton.ProtectionMode
import com.example.emergencybutton.ui.screens.ContactManagementScreen
import com.example.emergencybutton.ui.screens.EmergencyScreen
import com.example.emergencybutton.ui.screens.PinSettingsScreen
import com.example.emergencybutton.ui.theme.EmergencyButtonTheme
import com.example.emergencybutton.ui.account.AccountScreen
import com.example.emergencybutton.ui.account.AccountViewModel

@Composable
fun EmergencyApp(
    viewModel: EmergencyViewModel,
    accountViewModel: AccountViewModel,
    appContactsViewModel: AppContactsViewModel,
    onSelectMode: (ProtectionMode) -> Unit,
    onBeginEmergency: () -> Unit,
    buttonState: BleState,
    onScanButton: () -> Unit,
    onStartButton: () -> Unit,
    onStopButton: () -> Unit,
    onSelectButton: (String) -> Unit,
    onForgetButton: () -> Unit,
    onPickContact: () -> Unit,
    onStopButtonScan: () -> Unit
) {
    val state = viewModel.state
    // Leaving the page/account clears private lists and invalidates late callbacks.
    val contactsUser = if (state.screen == AppScreen.APP_CONTACTS) accountViewModel.state.user else null
    LaunchedEffect(contactsUser, state.screen) { appContactsViewModel.bind(contactsUser) }
    EmergencyButtonTheme(darkTheme = false, dynamicColor = false) {
        val backLabel = when (state.screen) {
            AppScreen.DASHBOARD -> null
            AppScreen.ACCOUNT -> "Back to settings"
            AppScreen.APP_CONTACTS -> "Back to SMS contacts"
            else -> "Back to dashboard"
        }
        val back: () -> Unit = {
            when (state.screen) {
                AppScreen.ACCOUNT -> { accountViewModel.leaveScreen(); viewModel.openSettings() }
                AppScreen.APP_CONTACTS -> viewModel.openContacts()
                AppScreen.CONTACTS -> viewModel.closeContacts()
                AppScreen.SETTINGS -> viewModel.closeSettings()
                AppScreen.BUTTON -> viewModel.closeButton()
                else -> Unit
            }
        }
        com.example.emergencybutton.ui.components.PageFrame(backLabel, back,
            message = when (state.screen) {
                AppScreen.APP_CONTACTS -> appContactsViewModel.state.status
                AppScreen.ACCOUNT -> accountViewModel.state.status
                AppScreen.DASHBOARD -> ""
                else -> state.status
            }) {
            when (state.screen) {
                AppScreen.BUTTON -> ButtonScreen(buttonState, onScanButton, onStartButton, onStopButton,
                    onSelectButton, onForgetButton, onStopButtonScan, viewModel::closeButton)
                AppScreen.APP_CONTACTS -> AppContactsScreen(appContactsViewModel, viewModel::openAccount, viewModel::openContacts)
                AppScreen.ACCOUNT -> AccountScreen(
                    state = accountViewModel.state,
                    onEmailChange = accountViewModel::updateEmail,
                    onPasswordChange = accountViewModel::updatePassword,
                    onConfirmationChange = accountViewModel::updateConfirmation,
                    onNameChange = accountViewModel::updateName,
                    onFormChange = accountViewModel::showForm,
                    onSubmit = accountViewModel::submit,
                    onSaveName = accountViewModel::saveName,
                    onSendVerification = accountViewModel::sendVerificationEmail,
                    onRefresh = accountViewModel::refreshUser,
                    onResetPassword = accountViewModel::resetSignedInPassword,
                    onSignOut = accountViewModel::signOut,
                    onBack = {
                        accountViewModel.leaveScreen()
                        viewModel.openSettings()
                    }
                )
                AppScreen.SETTINGS -> PinSettingsScreen(
                    hasExistingPin = state.hasCancellationPin,
                    currentPin = state.currentPinDraft,
                    newPin = state.newPinDraft,
                    confirmPin = state.confirmPinDraft,
                    status = state.status,
                    onCurrentPinChange = viewModel::updateCurrentPin,
                    onNewPinChange = viewModel::updateNewPin,
                    onConfirmPinChange = viewModel::updateConfirmPin,
                    onSave = viewModel::saveCancellationPin,
                    accountDescription = accountViewModel.state.user?.email ?: "Optional sign-in. SOS works without an account.",
                    onOpenAccount = viewModel::openAccount,
                    onBack = viewModel::closeSettings
                )
                AppScreen.CONTACTS -> ContactManagementScreen(
                    contacts = state.contacts,
                    contactDraft = state.contactDraft,
                    editingContact = state.editingContact,
                    status = state.status,
                    onContactDraftChange = viewModel::updateContactDraft,
                    onPickContact = onPickContact,
                    onSaveContact = viewModel::saveContact,
                    onEditContact = viewModel::startEditingContact,
                    onRemoveContact = viewModel::removeContact,
                    onCancelEdit = viewModel::cancelContactEdit,
                    onAppContacts = viewModel::openAppContacts,
                    onBack = viewModel::closeContacts
                )
                AppScreen.DASHBOARD -> EmergencyScreen(
                    contacts = state.contacts,
                    mode = state.protectionMode,
                    savedLocation = state.savedLocation,
                    incidentActive = state.incidentActive,
                    hasCancellationPin = state.hasCancellationPin,
                    cancellationPin = state.cancellationPinDraft,
                    status = state.status,
                    onModeChange = onSelectMode,
                    onEditContacts = viewModel::openContacts,
                    onOpenSettings = viewModel::openSettings,
                    onOpenButton = viewModel::openButton,
                    buttonStatus = buttonState.status,
                    onCancellationPinChange = viewModel::updateCancellationPin,
                    onCancelEmergency = viewModel::cancelEmergency,
                    onEmergencyClick = onBeginEmergency
                )
            }
        }
    }
}
