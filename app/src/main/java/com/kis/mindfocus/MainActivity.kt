package com.kis.mindfocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kis.mindfocus.navigation.MindFocusNavHost
import com.kis.mindfocus.ui.theme.MindFocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindFocusTheme {
                MindFocusNavHost()
            }
        }
    }
}
