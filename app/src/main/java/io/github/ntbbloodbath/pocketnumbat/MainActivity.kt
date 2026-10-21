package io.github.ntbbloodbath.pocketnumbat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ntbbloodbath.pocketnumbat.ui.NumbatScreen
import io.github.ntbbloodbath.pocketnumbat.ui.NumbatViewModel
import io.github.ntbbloodbath.pocketnumbat.ui.theme.PocketNumbatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketNumbatTheme {
                val viewModel: NumbatViewModel = viewModel()
                NumbatScreen(viewModel = viewModel)
            }
        }
    }
}
