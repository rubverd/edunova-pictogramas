package com.example.edunova

// ResultadoTurno.kt

data class ResultadoTurno(
    val letra: Char?,
    val palabraJugada: String?,
    val estado: EstadoTurno
)

// Usamos un 'enum' para definir los posibles estados de un turno de forma clara y segura.
enum class EstadoTurno {
    ACIERTO,
    FALLO
}

