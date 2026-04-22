package com.gptimage2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gptimage2.ui.screens.MainScreen
import com.gptimage2.ui.theme.GptImageTheme
import com.gptimage2.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GptImageTheme {
                val uiState by viewModel.uiState.collectAsState()
                MainScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}
