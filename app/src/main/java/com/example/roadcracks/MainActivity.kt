package com.example.roadcracks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.roadcracks.ui.theme.RoadCracksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoadCracksTheme {
                RoadCracksApp()
            }
        }
    }
}
