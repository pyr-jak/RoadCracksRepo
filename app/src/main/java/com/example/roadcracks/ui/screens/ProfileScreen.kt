package com.example.roadcracks.ui.screens

import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadcracks.RoadCracksApp
import com.example.roadcracks.data.UserProfile
import com.example.roadcracks.ui.components.ProfileAvatar
import com.example.roadcracks.ui.theme.RoadCracksTheme

@Composable
internal fun ProfileScreen(
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

@Preview(showBackground = true, showSystemUi = true, name = "05 - Profil", widthDp = 393, heightDp = 852)
@Composable
fun ProfileScreenPreview() {
    RoadCracksTheme { RoadCracksApp(initialScreen = 4) }
}
