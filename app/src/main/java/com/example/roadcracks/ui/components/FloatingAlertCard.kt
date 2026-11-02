package com.example.roadcracks.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.roadcracks.data.RoadAlert

@Composable
internal fun FloatingAlertCard(
    alert: RoadAlert,
    onClose: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(alert.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij pływające powiadomienie")
                    }
                }
            }
            Text("Miejsce: ${alert.place}", fontWeight = FontWeight.Medium)
            Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            if (onOpen != null) {
                TextButton(onClick = onOpen) { Text("Przejdź do alertów") }
            }
        }
    }
}
