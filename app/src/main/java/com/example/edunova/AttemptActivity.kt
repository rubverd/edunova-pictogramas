package com.example.edunova

import android.content.ContentValues.TAG
import androidx.compose.ui.semantics.text


import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.edunova.databinding.ActivityAttemptBinding // Importa el ViewBinding de tu layout de detalle
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.edunova.clases.StudentAttempt

class AttemptActivity : AppCompatActivity() {

    // 1. Declaración de variables
    private lateinit var binding: ActivityAttemptBinding
    private val db = FirebaseFirestore.getInstance() // Instancia de la base de datos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de ViewBinding para acceder a las vistas del XML de forma segura
        binding = ActivityAttemptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Opcional: Añadir un botón de "atrás" en la barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.buttonBack.setOnClickListener {
            finish()
        }

        // 2. Recuperar el ID de la partida que se pasó desde la lista
        val gameId = intent.getStringExtra("GAME_ID")

        // 3. Comprobar que el ID no es nulo
        if (gameId == null) {
            // Si no hay ID, no podemos mostrar nada. Mostramos un error y cerramos.
            showErrorState("Error: Partida no encontrada")
            // Podrías mostrar un Toast o simplemente cerrar la actividad.
            // finish()
            return
        }

        // 4. Iniciar la carga de los detalles de la partida desde Firestore
        loadGameDetails(gameId)
    }

    private fun loadGameDetails(gameId: String) {
        // Muestra el ProgressBar mientras se cargan los datos
        binding.progressBar.visibility = View.VISIBLE
        binding.contentGroup.visibility = View.GONE // Oculta el contenido principal

        // Accede a la colección "partidas" y busca el documento con el ID específico
        db.collection("intentos_alumnos") // Asegúrate de que "partidas" es el nombre correcto de tu colección
            .document(gameId)
            .get()
            .addOnSuccessListener { document ->
                // Oculta el ProgressBar una vez que se recibe una respuesta
                Log.i(TAG, "Respuesta de Firestore recibida para el documento: $gameId. ¿Existe?: ${document.exists()}")
                binding.progressBar.visibility = View.GONE
                binding.contentGroup.visibility = View.VISIBLE

                if (document != null && document.exists()) {
                    // Si el documento existe, lo convierte en un objeto StudentAttempt
                    val partida = document.toObject(StudentAttempt::class.java)

                    if (partida != null) {
                        // IMPORTANTE: El ID no está dentro del documento, así que lo asignamos manualmente
                        partida.id = document.id
                        // 5. Llama a la función para poblar la UI con los datos del objeto
                        populateUi(partida)
                    } else {
                        // Error en la conversión del objeto
                        showErrorState("Error al procesar los datos de la partida.")
                    }
                } else {
                    // El documento con ese ID no fue encontrado en la base de datos
                    showErrorState("No se encontraron los detalles para esta partida.")
                }
            }
            .addOnFailureListener { exception ->
                // Ocurrió un error de red o de permisos al intentar acceder a Firestore
                binding.progressBar.visibility = View.GONE
                showErrorState("Error de conexión: ${exception.message}")
            }
    }

    private fun populateUi(partida: StudentAttempt) {
        // Rellena la información general
        supportActionBar?.title = "Partida de ${partida.studentName}" // Cambia el título de la barra de acción
        binding.textViewDetailStudentName.text = "Estudiante: ${partida.studentName}"
        binding.textViewDetailScore.text = "Puntuación: ${partida.score}/${partida.totalQuestions}"
        binding.textViewDetailExerciseType.text = "Tipo: ${partida.exerciseType}"

        // Formatea la fecha y el tiempo para que sean más legibles
        binding.textViewDetailTimestamp.text = "Fecha: ${formatTimestamp(partida.timestamp)}"
        binding.textViewDetailTimeSpent.text = "Duración: ${formatTimeSpent(partida.timeSpentSeconds)}"

        // Configura el RecyclerView para la lista de ACIERTOS
        binding.recyclerViewAciertos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAciertos.adapter = WordAdapter(partida.aciertos)

        // Configura el RecyclerView para la lista de FALLOS
        binding.recyclerViewFallos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFallos.adapter = WordAdapter(partida.fallos)
    }


    // Funciones de ayuda para formatear los datos numéricos en texto legible
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy 'a las' HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatTimeSpent(totalSeconds: Long): String {
        if (totalSeconds < 0) return "N/A"
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}m ${seconds}s"
    }

    // Opcional: para que el botón "atrás" de la barra de acción funcione
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showErrorState(message: String) {
        // 1. Ocultar el contenido principal (la tarjeta, las listas, etc.)
        binding.contentGroup.visibility = View.GONE

        // 2. Ocultar la cabecera también
        binding.headerView.visibility = View.GONE

        // 3. Asegurarse de que la barra de progreso no se vea
        binding.progressBar.visibility = View.GONE

        // 4. Mostrar el TextView de mensajes y ponerle el texto
        binding.textViewStatusMessage.visibility = View.VISIBLE
        binding.textViewStatusMessage.text = message
    }
}
