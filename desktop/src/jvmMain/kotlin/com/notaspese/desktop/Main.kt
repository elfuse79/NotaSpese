package com.notaspese.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.notaspese.desktop.ui.AppContent
import com.notaspese.desktop.viewmodel.NotaSpeseViewModel

fun main() = application {
    val viewModel = NotaSpeseViewModel()
    Window(
        onCloseRequest = ::exitApplication,
        title = "NotaSpese - Gestione Note Spese v${com.notaspese.desktop.data.model.APP_VERSION}",
        state = rememberWindowState(width = 900.dp, height = 700.dp)
    ) {
        AppContent(viewModel = viewModel)
    }
}
