package com.example.roadcracks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.roadcracks.data.LocalData
import com.example.roadcracks.data.RoadAlert
import com.example.roadcracks.data.UserProfile
import com.example.roadcracks.ui.components.FloatingAlertCard
import com.example.roadcracks.ui.screens.AlertScreen
import com.example.roadcracks.ui.screens.HomeScreen
import com.example.roadcracks.ui.screens.MapScreen
import com.example.roadcracks.ui.screens.MapujScreen
import com.example.roadcracks.ui.screens.ProfileScreen
import com.example.roadcracks.utils.loadAvatar
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RoadCracksApp(initialScreen: Int = 0) {
    var selectedItem by rememberSaveable(initialScreen) { mutableIntStateOf(initialScreen) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val store = remember(context, isPreview) {
        if (isPreview) null else LocalData(context.applicationContext)
    }
    var profile by remember { mutableStateOf(store?.loadProfile() ?: UserProfile()) }
    var observedPlace by remember { mutableStateOf(store?.place.orEmpty()) }
    var alerts by remember { mutableStateOf(store?.loadAlerts() ?: emptyList()) }
    var floatingAlert by remember { mutableStateOf<RoadAlert?>(null) }
    var floatingAlertVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(floatingAlert?.id) {
        if (floatingAlert != null) {
            delay(8_000)
            floatingAlertVisible = false
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, withDismissAction = true)
        }
    }

    fun updateAlerts(updated: List<RoadAlert>) {
        alerts = updated.take(50)
        store?.saveAlerts(alerts)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState, modifier = Modifier.imePadding()) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                if (selectedItem == 1) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Szukaj adresu na mapie...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Szukaj") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Główna") },
                        label = { Text("Główna") },
                        selected = selectedItem == 0,
                        onClick = { selectedItem = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
                        label = { Text("Mapa") },
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AddRoad, contentDescription = "Dodaj") },
                        label = { Text("Mapuj") },
                        selected = selectedItem == 2,
                        onClick = { selectedItem = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Powiadomienia") },
                        label = { Text("Alert") },
                        selected = selectedItem == 3,
                        onClick = { selectedItem = 3 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil") },
                        selected = selectedItem == 4,
                        onClick = { selectedItem = 4 }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItem) {
                0 -> HomeScreen(
                    profile = profile,
                    alerts = alerts,
                    observedPlace = observedPlace,
                    onOpenMapuj = { selectedItem = 2 },
                    onOpenAlerts = { selectedItem = 3 }
                )
                1 -> MapScreen(context)
                2 -> MapujScreen()
                3 -> AlertScreen(
                    alerts = alerts,
                    observedPlace = observedPlace,
                    onSavePlace = { place ->
                        observedPlace = place.trim()
                        store?.place = observedPlace
                        showMessage("Zapisano obserwowane miejsce")
                    },
                    onRead = { id ->
                        updateAlerts(alerts.map { if (it.id == id) it.copy(isRead = true) else it })
                    },
                    onReadAll = { updateAlerts(alerts.map { it.copy(isRead = true) }) },
                    onDelete = { id -> updateAlerts(alerts.filterNot { it.id == id }) },
                    onDemoAlert = {
                        val alert = RoadAlert(
                            id = UUID.randomUUID().toString(),
                            title = "Przykładowy alert drogowy",
                            message = "Przykład: uszkodzenie nawierzchni. To test wyglądu powiadomień.",
                            place = observedPlace.ifBlank { "Nie określono miejsca" },
                            createdAt = System.currentTimeMillis()
                        )
                        updateAlerts(listOf(alert) + alerts)
                        floatingAlert = alert
                        floatingAlertVisible = true
                    }
                )
                4 -> ProfileScreen(
                    profile = profile,
                    onSave = { updated ->
                        profile = updated
                        store?.saveProfile(updated)
                        showMessage("Zapisano dane profilu")
                    },
                    onPhotoSelected = { uri ->
                        scope.launch {
                            val bitmap = withContext(Dispatchers.IO) { loadAvatar(context, uri) }
                            if (bitmap == null) {
                                if (profile.photoUri != uri) {
                                    runCatching {
                                        context.contentResolver.releasePersistableUriPermission(
                                            Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                                }
                                showMessage("Nie udało się wczytać zdjęcia. Wybierz inne.")
                            } else {
                                val previousUri = profile.photoUri
                                profile = profile.copy(photoUri = uri)
                                store?.saveProfile(profile)
                                if (previousUri.isNotBlank() && previousUri != uri) {
                                    runCatching {
                                        context.contentResolver.releasePersistableUriPermission(
                                            Uri.parse(previousUri), Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                                }
                                showMessage("Zmieniono zdjęcie profilowe")
                            }
                        }
                    },
                    onMessage = ::showMessage
                )
            }
            if (floatingAlertVisible) {
                Box(Modifier.align(Alignment.TopCenter).padding(16.dp)) {
                    floatingAlert?.let { alert ->
                        FloatingAlertCard(
                            alert = alert,
                            onClose = { floatingAlertVisible = false },
                            onOpen = {
                                selectedItem = 3
                                floatingAlertVisible = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoadCracksMainLayout() {
    RoadCracksApp()
}
