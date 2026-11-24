package com.aurora.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.aurora.ai.ui.AuroraTheme
import com.aurora.ai.ui.ChatScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AuroraTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ChatScreen()
                }
            }
        }
    }
}
