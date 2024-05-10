package com.example.bhavna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bhavna.ui.BhavnaApp
import com.example.bhavna.ui.theme.BhavnaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BhavnaTheme {
                BhavnaApp()
            }
        }
    }
}