package com.nth.thechatptit.ui.common.snackbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nth.thechatptit.ui.theme.*

@Composable
fun AppSnackbar(
    snackbarHostState: SnackbarHostState,
    messenger: Messenger
) {
    MessageHandler(snackbarHostState, messenger)

    val messageType = messenger.messageType.collectAsStateWithLifecycle()

    val color = when (messageType.value) {
        Message.Type.SUCCESS -> MaterialTheme.colorScheme.success
        Message.Type.ERROR -> MaterialTheme.colorScheme.error
        Message.Type.WARNING -> MaterialTheme.colorScheme.warning
        Message.Type.INFO -> MaterialTheme.colorScheme.info
    }

    SnackbarHost(hostState = snackbarHostState) {
        Snackbar(
            snackbarData = it,
            containerColor = color,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            actionColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}
