package com.example.roadcracks.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("pl", "PL")).format(Date(timestamp))
