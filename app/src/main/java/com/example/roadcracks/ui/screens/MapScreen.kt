package com.example.roadcracks.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadcracks.RoadCracksApp
import com.example.roadcracks.ui.components.UserLocationMap
import com.example.roadcracks.ui.theme.RoadCracksTheme

@Composable
fun MapScreen(context: android.content.Context) {
    Box(modifier = Modifier.fillMaxSize()) {
        UserLocationMap(modifier = Modifier.fillMaxSize())

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 75.dp)
                .size(width = 75.dp, height = 140.dp)
                .clickable {
                    Toast.makeText(context, "Kliknięto lewy owal!", Toast.LENGTH_SHORT).show()
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.height(4.dp))
                Text("200 m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 75.dp)
                .size(width = 75.dp, height = 140.dp)
                .clickable {
                    Toast.makeText(context, "Kliknięto prawy owal!", Toast.LENGTH_SHORT).show()
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Speed, contentDescription = null)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Km/h", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "02 - Mapa", widthDp = 393, heightDp = 852)
@Composable
fun MapScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 1) }
}
