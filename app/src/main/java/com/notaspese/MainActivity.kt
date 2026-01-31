package com.notaspese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notaspese.ui.navigation.NotaSpeseNavigation
import com.notaspese.ui.theme.NotaSpeseTheme
import com.notaspese.viewmodel.NotaSpeseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotaSpeseTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: NotaSpeseViewModel = viewModel()
                    NotaSpeseNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
