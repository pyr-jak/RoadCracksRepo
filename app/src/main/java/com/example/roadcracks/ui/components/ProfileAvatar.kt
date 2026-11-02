package com.example.roadcracks.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.roadcracks.utils.loadAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ProfileAvatar(photoUri: String, modifier: Modifier = Modifier) {
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
