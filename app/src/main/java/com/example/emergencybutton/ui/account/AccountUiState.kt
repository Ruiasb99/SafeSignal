package com.example.emergencybutton.ui.account

import com.example.emergencybutton.domain.AccountUser

enum class AccountForm { SIGN_IN, REGISTER, RESET_PASSWORD }

data class AccountUiState(
    val configured: Boolean,
    val user: AccountUser? = null,
    val form: AccountForm = AccountForm.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val nameDraft: String = "",
    val busy: Boolean = false,
    val status: String = "",
    val isError: Boolean = false
)
