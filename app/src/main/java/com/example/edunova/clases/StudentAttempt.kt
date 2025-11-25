package com.example.edunova.clases

import com.google.firebase.Timestamp

// Esta clase representa un documento de la colección "intentos_alumnos"
data class StudentAttempt(
    val id: String = "", // ID del documento
    val studentUid: String = "",
    val exerciseType: String = "", // "vocabulario", "silabas", etc.
    val score: Int = 0, // Ej: 8
    val totalQuestions: Int = 0, // Ej: 10
    val timestamp: Long = 0, // Fecha en milisegundos
    // Puedes añadir 'details' más tarde si quieres mostrar el desglose exacto
) {
    // Helper para mostrar la nota bonita (Ej: "8/10")
    fun getScoreString(): String {
        return "$score / $totalQuestions"
    }

    // Helper para mostrar fecha (puedes usar SimpleDateFormat aquí si quieres)
    fun getDateString(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}