package com.notaspese.ui.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notaspese.ui.screens.*
import com.notaspese.viewmodel.NotaSpeseViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateNotaSpese : Screen("create_nota_spese")
    object EditNotaSpese : Screen("edit_nota_spese/{notaSpeseId}") { fun createRoute(id: Long) = "edit_nota_spese/$id" }
    object Detail : Screen("detail/{notaSpeseId}") { fun createRoute(id: Long) = "detail/$id" }
    object AddSpesa : Screen("add_spesa/{notaSpeseId}") { fun createRoute(id: Long) = "add_spesa/$id" }
    object EditSpesa : Screen("edit_spesa/{notaSpeseId}/{spesaId}") { fun createRoute(notaSpeseId: Long, spesaId: Long) = "edit_spesa/$notaSpeseId/$spesaId" }
}

@Composable
fun NotaSpeseNavigation(viewModel: NotaSpeseViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noteSpese by viewModel.allNoteSpeseConSpese.collectAsStateWithLifecycle()
    val currentNotaSpese by viewModel.currentNotaSpese.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val type = context.contentResolver.getType(it) ?: ""
                val name = runCatching {
                    context.contentResolver.query(it, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    }
                }.getOrNull()
                val isNotaSpese = type == "application/zip" || name?.endsWith(".notaspese", true) == true
                val imported = if (isNotaSpese) viewModel.importFromNotaSpeseUri(it) else viewModel.importFromCsv(it)
                if (imported != null) {
                    viewModel.importAndSave(imported)
                    Toast.makeText(context, "Import completato: ${imported.notaSpese.nomeCognome}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Errore nell'import (usare CSV o file .notaspese)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { 
            HomeScreen(
                noteSpese, 
                { navController.navigate(Screen.CreateNotaSpese.route) }, 
                { id -> viewModel.loadNotaSpese(id); navController.navigate(Screen.Detail.createRoute(id)) },
                { notaConSpese -> viewModel.deleteNotaSpeseWithFolder(notaConSpese) },
                onImportClick = { importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv", "application/zip", "*/*")) }
            ) 
        }
        
        composable(Screen.CreateNotaSpese.route) { 
            CreateNotaSpeseScreen(
                onNavigateBack = { navController.popBackStack() }, 
                onSave = { nota -> viewModel.createNotaSpese(nota) { id -> navController.popBackStack(); viewModel.loadNotaSpese(id); navController.navigate(Screen.Detail.createRoute(id)) } },
                existingNota = null
            ) 
        }
        
        composable(Screen.EditNotaSpese.route, arguments = listOf(navArgument("notaSpeseId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("notaSpeseId") ?: return@composable
            LaunchedEffect(id) { viewModel.loadNotaSpese(id) }
            val notaToEdit = currentNotaSpese?.notaSpese
            
            if (notaToEdit != null) {
                CreateNotaSpeseScreen(
                    onNavigateBack = { navController.popBackStack() }, 
                    onSave = { nota -> 
                        viewModel.updateNotaSpese(nota)
                        navController.popBackStack()
                    },
                    existingNota = notaToEdit
                )
            }
        }
        
        composable(Screen.Detail.route, arguments = listOf(navArgument("notaSpeseId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("notaSpeseId") ?: return@composable
            LaunchedEffect(id) { viewModel.loadNotaSpese(id) }
            DetailScreen(
                notaSpeseConSpese = currentNotaSpese, 
                onNavigateBack = { navController.popBackStack() }, 
                onAddSpesa = { navController.navigate(Screen.AddSpesa.createRoute(id)) }, 
                onEditSpesa = { spesa -> navController.navigate(Screen.EditSpesa.createRoute(id, spesa.id)) }, 
                onDeleteSpesa = { viewModel.deleteSpesa(it) }, 
                onEditAnticipo = { viewModel.updateAnticipo(id, it) },
                onEditNota = { navController.navigate(Screen.EditNotaSpese.createRoute(id)) }
            )
        }
        
        composable(Screen.AddSpesa.route, arguments = listOf(navArgument("notaSpeseId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("notaSpeseId") ?: return@composable
            AddSpesaScreen(id, { navController.popBackStack() }, { spesa -> viewModel.addSpesa(spesa) { navController.popBackStack() } }, existingSpesa = null)
        }
        
        composable(Screen.EditSpesa.route, arguments = listOf(navArgument("notaSpeseId") { type = NavType.LongType }, navArgument("spesaId") { type = NavType.LongType })) { entry ->
            val notaId = entry.arguments?.getLong("notaSpeseId") ?: return@composable
            val spesaId = entry.arguments?.getLong("spesaId") ?: return@composable
            LaunchedEffect(notaId) { viewModel.loadNotaSpese(notaId) }
            val existingSpesa = currentNotaSpese?.spese?.find { it.id == spesaId }
            when {
                existingSpesa != null -> AddSpesaScreen(
                    notaSpeseId = notaId,
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { spesa -> viewModel.updateSpesa(spesa); navController.popBackStack() },
                    existingSpesa = existingSpesa
                )
                currentNotaSpese == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}
