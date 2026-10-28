package com.example.roadcracks

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadcracks.ui.theme.RoadCracksTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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

private data class UserProfile(
    val name: String = "Użytkownik",
    val email: String = "",
    val gender: String = "",
    val photoUri: String = ""
)

private data class RoadAlert(
    val id: String,
    val title: String,
    val message: String,
    val place: String,
    val createdAt: Long,
    val isRead: Boolean = false
)

private class LocalData(context: Context) {
    private val preferences = context.getSharedPreferences("roadcracks_ui", Context.MODE_PRIVATE)

    fun loadProfile() = UserProfile(
        name = preferences.getString("name", "Użytkownik").orEmpty(),
        email = preferences.getString("email", "").orEmpty(),
        gender = preferences.getString("gender", "").orEmpty(),
        photoUri = preferences.getString("photo_uri", "").orEmpty()
    )

    fun saveProfile(profile: UserProfile) {
        preferences.edit()
            .putString("name", profile.name)
            .putString("email", profile.email)
            .putString("gender", profile.gender)
            .putString("photo_uri", profile.photoUri)
            .apply()
    }

    var place: String
        get() = preferences.getString("observed_place", "").orEmpty()
        set(value) { preferences.edit().putString("observed_place", value).apply() }

    fun loadAlerts(): List<RoadAlert> = runCatching {
        val array = JSONArray(preferences.getString("alerts", "[]") ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                RoadAlert(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    message = item.getString("message"),
                    place = item.getString("place"),
                    createdAt = item.getLong("createdAt"),
                    isRead = item.optBoolean("isRead", false)
                )
            }.getOrNull()
        }.take(50)
    }.getOrDefault(emptyList())

    fun saveAlerts(alerts: List<RoadAlert>) {
        val array = JSONArray()
        alerts.take(50).forEach { alert ->
            array.put(JSONObject().apply {
                put("id", alert.id)
                put("title", alert.title)
                put("message", alert.message)
                put("place", alert.place)
                put("createdAt", alert.createdAt)
                put("isRead", alert.isRead)
            })
        }
        preferences.edit().putString("alerts", array.toString()).apply()
    }
}

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

@Composable
private fun FloatingAlertCard(
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

@Composable
private fun HomeScreen(
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

@Composable
private fun StatCard(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlertScreen(
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

@Composable
private fun ProfileScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onPhotoSelected: (String) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    var email by rememberSaveable(profile.email) { mutableStateOf(profile.email) }
    var gender by rememberSaveable(profile.gender) { mutableStateOf(profile.gender) }
    var genderMenuExpanded by remember { mutableStateOf(false) }
    var saveAttempted by rememberSaveable { mutableStateOf(false) }
    val nameError = saveAttempted && name.isBlank()
    val emailError = saveAttempted && email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val genderError = saveAttempted && gender.isBlank()
    val photoPicker = if (isPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.isSuccess
            if (persisted) onPhotoSelected(uri.toString())
            else onMessage("Nie udało się zapamiętać dostępu do zdjęcia. Wybierz inne.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Twój profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Twoje zdjęcie i dane użytkownika.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(112.dp).clickable(
                            role = Role.Button,
                            onClickLabel = "Zmień zdjęcie profilowe"
                        ) { photoPicker?.launch(arrayOf("image/*")) }
                    ) {
                        ProfileAvatar(profile.photoUri, Modifier.fillMaxSize())
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.BottomEnd).size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Zmień zdjęcie profilowe",
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(name.ifBlank { "Użytkownik" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(email.ifBlank { "Nie podano adresu e-mail" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Płeć: ${gender.ifBlank { "Nie wybrano" }}",
                        style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { photoPicker?.launch(arrayOf("image/*")) }) { Text("Zmień zdjęcie") }
                }
            }
        }
        item {
            Text("Płeć", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { genderMenuExpanded = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text(gender.ifBlank { "Wybierz płeć" }, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = genderMenuExpanded,
                    onDismissRequest = { genderMenuExpanded = false }) {
                    listOf("Mężczyzna", "Kobieta").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { gender = option; genderMenuExpanded = false }
                        )
                    }
                }
            }
            if (genderError) {
                Text("Wybierz jedną opcję", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50) },
                label = { Text("Nazwa użytkownika") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                isError = nameError,
                supportingText = { if (nameError) Text("Podaj nazwę użytkownika") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.take(254) },
                label = { Text("Adres e-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError,
                supportingText = { if (emailError) Text("Podaj poprawny adres e-mail") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    saveAttempted = true
                    val emailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                    if (name.isNotBlank() && emailValid && gender.isNotBlank()) {
                        onSave(profile.copy(name = name.trim(), email = email.trim(), gender = gender))
                    }
                }
            ) { Text("Zapisz zmiany") }
        }
    }
}

@Composable
private fun ProfileAvatar(photoUri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = null
        if (photoUri.isNotBlank()) {
            value = withContext(Dispatchers.IO) { loadAvatar(context, photoUri) }
        }
    }
    Box(
        modifier = modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(bitmap = loadedBitmap.asImageBitmap(), contentDescription = "Zdjęcie profilowe",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.Person, contentDescription = "Domyślne zdjęcie profilowe",
                modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun loadAvatar(context: Context, value: String): Bitmap? = runCatching {
    val uri = Uri.parse(value)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val largest = maxOf(info.size.width, info.size.height)
            if (largest > 512) {
                val scale = 512f / largest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    } else {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 512) {
            options.inSampleSize *= 2
        }
        options.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}.getOrNull()

@Composable
private fun EmptyCard(icon: ImageVector, title: String, message: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("pl", "PL")).format(Date(timestamp))

@Composable
fun MapScreen(context: android.content.Context) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE3E8E5)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Mapa",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Widok Mapy",
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

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

@Composable
fun MapujScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AddRoad, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tryb Mapowania", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* TODO */ }) {
                Text("Rozpocznij Nagrywanie Trasy")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "01 - Główna", widthDp = 393, heightDp = 852)
@Composable
fun HomeScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 0) }
}

@Preview(showBackground = true, showSystemUi = true, name = "02 - Mapa", widthDp = 393, heightDp = 852)
@Composable
fun MapScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 1) }
}

@Preview(showBackground = true, showSystemUi = true, name = "03 - Mapuj", widthDp = 393, heightDp = 852)
@Composable
fun MapujScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 2) }
}

@Preview(showBackground = true, showSystemUi = true, name = "04 - Alert", widthDp = 393, heightDp = 852)
@Composable
fun AlertScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 3) }
}

@Preview(showBackground = true, showSystemUi = true, name = "05 - Profil", widthDp = 393, heightDp = 852)
@Composable
fun ProfileScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 4) }
}
