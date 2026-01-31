package com.notaspese.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notaspese.ui.screens.*
import com.notaspese.viewmodel.NotaSpeseViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateNotaSpese : Screen("create_nota_spese")
    object EditNotaSpese : Screen("edit_nota_spese/{notaSpeseId}") { fun createRoute(id: Long) = "edit_nota_spese/$id" }
    object Detail : Screen("detail/{notaSpeseId}") { fun createRoute(id: Long) = "detail/$id" }
    object AddSpesa : Screen("add_spesa/{notaSpeseId}") { fun createRoute(id: Long) = "add_spesa/$id" }
}

@Composable
fun NotaSpeseNavigation(viewModel: NotaSpeseViewModel) {
    val navController = rememberNavController()
    val noteSpese by viewModel.allNoteSpeseConSpese.collectAsStateWithLifecycle()
    val currentNotaSpese by viewModel.currentNotaSpese.collectAsStateWithLifecycle()
    
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { 
            HomeScreen(noteSpese, { navController.navigate(Screen.CreateNotaSpese.route) }, { id -> viewModel.loadNotaSpese(id); navController.navigate(Screen.Detail.createRoute(id)) }) 
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
                onDeleteSpesa = { viewModel.deleteSpesa(it) }, 
                onEditAnticipo = { viewModel.updateAnticipo(id, it) },
                onEditNota = { navController.navigate(Screen.EditNotaSpese.createRoute(id)) }
            )
        }
        
        composable(Screen.AddSpesa.route, arguments = listOf(navArgument("notaSpeseId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("notaSpeseId") ?: return@composable
            AddSpesaScreen(id, { navController.popBackStack() }, { spesa -> viewModel.addSpesa(spesa) { navController.popBackStack() } })
        }
    }
}
