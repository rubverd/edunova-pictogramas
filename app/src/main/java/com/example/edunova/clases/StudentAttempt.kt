package com.example.edunova.clases

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StudentAttempt(
    var id: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val exerciseType: String = "", // "vocabulario", "frases", etc.
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val timestamp: Long = 0,       // FECHA GENERAL DEL INTENTO
    val timeSpentSeconds: Long = 0, // NUEVO: TIEMPO QUE TARDÓ
    val aciertos: MutableList<String> = mutableListOf(),
    val fallos: MutableList<String> = mutableListOf()
) {
    // Helper para mostrar la nota ej: "8/10"
    fun getScoreString(): String {
        return "$score/$totalQuestions"
    }

    // Helper para formatear la fecha ej: "29/11/2023 10:30"
    fun getDateString(): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    // Helper para formatear el tiempo ej: "02:15"
    fun getDurationString(): String {
        val minutes = timeSpentSeconds / 60
        val seconds = timeSpentSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}