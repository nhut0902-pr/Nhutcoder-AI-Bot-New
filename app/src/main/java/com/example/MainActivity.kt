package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.ChatViewModel
import com.example.ui.SpaceZScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ChatViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Handle shortcut or widget click intent on launch
    handleIntent(intent)

    setContent {
      MyApplicationTheme {
        SpaceZScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    val action = intent?.action
    if (action != null && action.startsWith("com.example.ACTION_")) {
      viewModel.triggerAction(action)
    }
    val widgetExtra = intent?.getStringExtra("WIDGET_ACTION")
    if (widgetExtra != null) {
      viewModel.triggerAction(widgetExtra)
    }
  }
}
