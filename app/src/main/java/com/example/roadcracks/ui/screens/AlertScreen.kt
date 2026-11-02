package com.example.roadcracks.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadcracks.RoadCracksApp
import com.example.roadcracks.data.RoadAlert
import com.example.roadcracks.ui.components.FloatingAlertCard
import com.example.roadcracks.ui.theme.RoadCracksTheme
import com.example.roadcracks.utils.formatDate

@Composable
internal fun AlertScreen(
    alerts: List<RoadAlert>,
    observedPlace: String,
    onSavePlace: (String) -> Unit,
    onRead: (String) -> Unit,
    onReadAll: () -> Unit,
    onDelete: (String) -> Unit,
    onDemoAlert: () -> Unit
) {
    var placeDraft by rememberSaveable(observedPlace) { mutableStateOf(observedPlace) }
    var openAlertId by rememberSaveable { mutableStateOf<String?>(null) }
    val openAlert = alerts.firstOrNull { it.id == openAlertId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Alerty i powiadomienia", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("Obserwuj wybrane miejsce i sprawdzaj komunikaty.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Obserwowane miejsce", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = placeDraft,
                        onValueChange = { placeDraft = it.take(120) },
                        label = { Text("Miasto lub ulica") },
                        placeholder = { Text("np. Kraków, ul. Długa") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { onSavePlace(placeDraft) },
                        enabled = placeDraft.trim() != observedPlace) { Text("Zapisz miejsce") }
                }
            }
        }
        item {
            OutlinedButton(onClick = onDemoAlert, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pokaż przykładowy alert")
            }
            Spacer(Modifier.height(6.dp))
            Text("Przycisk dodaje testowy komunikat i pokazuje pływające okienko.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Powiadomienia (${alerts.size})", style = MaterialTheme.typography.titleLarge)
            if (alerts.any { !it.isRead }) {
                TextButton(onClick = onReadAll) { Text("Oznacz wszystkie jako przeczytane") }
            }
        }
        if (alerts.isEmpty()) {
            item {
                Text("Podgląd powiadomienia", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                FloatingAlertCard(
                    alert = RoadAlert(
                        id = "preview",
                        title = "Przykładowe ostrzeżenie",
                        message = "Tutaj pojawi się opis ostrzeżenia drogowego. To podgląd wyglądu okienka.",
                        place = observedPlace.ifBlank { "Wybrane miasto lub ulica" },
                        createdAt = 0
                    )
                )
            }
        }
        items(alerts, key = { it.id }) { alert ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (alert.isRead) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(alert.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelete(alert.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Usuń powiadomienie")
                        }
                    }
                    if (!alert.isRead) {
                        Text("NOWE", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(alert.message)
                    Text("Miejsce: ${alert.place}", style = MaterialTheme.typography.bodyMedium)
                    Text(formatDate(alert.createdAt), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {
                        onRead(alert.id)
                        openAlertId = alert.id
                    }) { Text("Szczegóły") }
                }
            }
        }
    }

    if (openAlert != null) {
        AlertDialog(
            onDismissRequest = { openAlertId = null },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            title = { Text(openAlert.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(openAlert.message)
                    Text("Miejsce: ${openAlert.place}")
                    Text(formatDate(openAlert.createdAt))
                }
            },
            confirmButton = { TextButton(onClick = { openAlertId = null }) { Text("Zamknij") } }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "04 - Alert", widthDp = 393, heightDp = 852)
@Composable
fun AlertScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 3) }
}
