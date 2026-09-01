package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FlappyBirdGameScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Unlock maximum display refresh rate (90Hz / 120Hz / 144Hz) for ultra-smooth gameplay
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      try {
        val currentDisplay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
          display
        } else {
          @Suppress("DEPRECATION")
          windowManager.defaultDisplay
        }
        val maxMode = currentDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
        if (maxMode != null) {
          val params = window.attributes
          params.preferredDisplayModeId = maxMode.modeId
          window.attributes = params
        }
      } catch (_: Exception) {
        // Fallback gracefully on standard refresh rate
      }
    }

    setContent {
      MyApplicationTheme {
        val gameViewModel: GameViewModel = viewModel()
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color.Black
        ) {
          FlappyBirdGameScreen(viewModel = gameViewModel)
        }
      }
    }
  }
}

