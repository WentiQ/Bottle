package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.repository.FirebaseFounderRepository
import com.example.ui.FounderApp
import com.example.ui.FounderViewModel
import com.example.ui.FounderViewModelFactory
import com.example.ui.theme.FounderSyncTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FounderViewModel by viewModels {
        val repository = FirebaseFounderRepository(applicationContext)
        FounderViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FounderSyncTheme {
                FounderApp(viewModel = viewModel)
            }
        }
    }
}
