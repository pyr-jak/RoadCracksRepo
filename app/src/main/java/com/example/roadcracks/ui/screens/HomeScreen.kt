package com.example.roadcracks.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadcracks.RoadCracksApp
import com.example.roadcracks.data.RoadAlert
import com.example.roadcracks.data.UserProfile
import com.example.roadcracks.ui.components.EmptyCard
import com.example.roadcracks.ui.components.StatCard
import com.example.roadcracks.ui.theme.RoadCracksTheme
import com.example.roadcracks.utils.formatDate

@Composable
internal fun HomeScreen(
    profile: UserProfile,
    alerts: List<RoadAlert>,
    observedPlace: String,
    onOpenMapuj: () -> Unit,
    onOpenAlerts: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("RoadCracks · Główna", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Podsumowanie aktywności", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("Witaj, ${profile.name}!", 
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Twoje statystyki", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("0", "Zapisane trasy", Icons.Default.AddRoad, Modifier.weight(1f))
                StatCard("0,0 km", "Zmapowane drogi", Icons.Default.Map, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(alerts.size.toString(), "Powiadomienia", Icons.Default.Notifications, Modifier.weight(1f))
                StatCard(alerts.count { !it.isRead }.toString(), "Nieprzeczytane", Icons.Default.Mail, Modifier.weight(1f))
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.AddRoad, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Każda trasa ma znaczenie", style = MaterialTheme.typography.titleLarge)
                    Text("Przejdź do mapowania i poznawaj drogi w swojej okolicy.")
                    Button(onClick = onOpenMapuj) { Text("Przejdź do Mapuj") }
                }
            }
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Obserwowane miejsce", style = MaterialTheme.typography.labelLarge)
                        Text(observedPlace.ifBlank { "Nie ustawiono miejsca" })
                    }
                    TextButton(onClick = onOpenAlerts) { Text("Zmień") }
                }
            }
        }
        item {
            Text("Ostatnia aktywność", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
        }
        if (alerts.isEmpty()) {
            item {
                EmptyCard(Icons.Default.History, "Brak zapisanej aktywności",
                    "Tutaj zobaczysz ostatnie powiadomienia.")
            }
        } else {
            items(alerts.take(3), key = { it.id }) { alert ->
                OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(alert.title, fontWeight = FontWeight.Medium)
                        Text(alert.place, style = MaterialTheme.typography.bodyMedium)
                        Text(formatDate(alert.createdAt), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onOpenAlerts, modifier = Modifier.fillMaxWidth()) {
                Text("Przejdź do powiadomień")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "01 - Główna", widthDp = 393, heightDp = 852)
@Composable
fun HomeScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 0) }
}
