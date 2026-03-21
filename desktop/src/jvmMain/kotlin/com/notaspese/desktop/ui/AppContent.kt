package com.notaspese.desktop.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.notaspese.desktop.ui.screens.*
import com.notaspese.desktop.viewmodel.NotaSpeseViewModel

sealed class AppScreen {
    object Home : AppScreen()
    object CreateNota : AppScreen()
    data class EditNota(val id: Long) : AppScreen()
    data class Detail(val id: Long) : AppScreen()
    data class AddSpesa(val notaSpeseId: Long) : AppScreen()
    data class EditSpesa(val notaSpeseId: Long, val spesa: com.notaspese.desktop.data.model.Spesa) : AppScreen()
}

@Composable
fun AppContent(viewModel: NotaSpeseViewModel) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    val noteSpese by viewModel.allNoteSpeseConSpese.collectAsState()
    val currentNota by viewModel.currentNotaSpese.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val screen = currentScreen) {
                AppScreen.Home -> HomeScreen(
                    noteSpese = noteSpese,
                    onNavigateToCreate = { currentScreen = AppScreen.CreateNota },
                    onNavigateToDetail = { id ->
                        viewModel.loadNotaSpese(id)
                        currentScreen = AppScreen.Detail(id)
                    },
                    onDeleteNota = {
                        viewModel.deleteNotaSpeseWithFolder(it)
                        currentScreen = AppScreen.Home
                    },
                    onImportFromFolder = { folder ->
                        viewModel.importFromFolder(folder)?.let { viewModel.importAndSave(it) }
                    },
                    onImportFromNotaSpeseFile = { file ->
                        viewModel.importFromNotaSpeseFile(file)?.let { viewModel.importAndSave(it) }
                    },
                    onImportFromPath = { path ->
                        viewModel.importFromPath(path)?.let { viewModel.importAndSave(it) }
                    }
                )
                AppScreen.CreateNota -> CreateNotaSpeseScreen(
                    onNavigateBack = { currentScreen = AppScreen.Home },
                    onSave = { nota ->
                        viewModel.createNotaSpese(nota) { id ->
                            viewModel.loadNotaSpese(id)
                            currentScreen = AppScreen.Detail(id)
                        }
                    },
                    existingNota = null
                )
                is AppScreen.EditNota -> currentNota?.notaSpese?.let { nota ->
                    CreateNotaSpeseScreen(
                        onNavigateBack = { currentScreen = AppScreen.Detail(screen.id) },
                        onSave = { viewModel.updateNotaSpese(it); currentScreen = AppScreen.Detail(screen.id) },
                        existingNota = nota
                    )
                } ?: run {
                    currentScreen = AppScreen.Home
                }
                is AppScreen.Detail -> DetailScreen(
                    notaSpeseConSpese = currentNota,
                    onNavigateBack = { currentScreen = AppScreen.Home },
                    onAddSpesa = { currentScreen = AppScreen.AddSpesa(screen.id) },
                    onEditSpesa = { spesa -> currentScreen = AppScreen.EditSpesa(screen.id, spesa) },
                    onDeleteSpesa = { viewModel.deleteSpesa(it) },
                    onEditAnticipo = { viewModel.updateAnticipo(screen.id, it) },
                    onEditNota = { currentScreen = AppScreen.EditNota(screen.id) },
                    onExport = { viewModel.exportPdfAndCsv(it) }
                )
                is AppScreen.AddSpesa -> AddSpesaScreen(
                    notaSpeseId = screen.notaSpeseId,
                    notaFolder = viewModel.getNotaFolder(screen.notaSpeseId),
                    onNavigateBack = { currentScreen = AppScreen.Detail(screen.notaSpeseId) },
                    onSave = { spesa ->
                        viewModel.addSpesa(spesa) {
                            currentScreen = AppScreen.Detail(screen.notaSpeseId)
                        }
                    }
                )
                is AppScreen.EditSpesa -> AddSpesaScreen(
                    notaSpeseId = screen.notaSpeseId,
                    notaFolder = viewModel.getNotaFolder(screen.notaSpeseId),
                    onNavigateBack = { currentScreen = AppScreen.Detail(screen.notaSpeseId) },
                    onSave = { spesa ->
                        viewModel.updateSpesa(spesa)
                        currentScreen = AppScreen.Detail(screen.notaSpeseId)
                    },
                    existingSpesa = screen.spesa
                )
            }
        }
    }
}
