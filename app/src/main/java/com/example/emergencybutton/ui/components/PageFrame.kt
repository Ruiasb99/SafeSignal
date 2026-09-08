package com.example.emergencybutton.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Owns system/keyboard insets once; navigation stays outside the scrolling page. */
@Composable
fun PageFrame(backLabel: String?, onBack: () -> Unit, message: String = "", content: @Composable () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (message.isNotBlank() && message != "Ready" && message != "App contacts are up to date.") {
            snackbar.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Long)
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding().imePadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content()
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
        if (backLabel != null) {
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 4.dp) {
                FilledTonalButton(onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                        .heightIn(min = 48.dp)) {
                    Text("←")
                    Spacer(Modifier.width(8.dp))
                    Text(backLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
