package com.example.edunova

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.edunova.databinding.ActivityJuegoFrasesBinding
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.Locale

class JuegoFrasesActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityJuegoFrasesBinding
    private lateinit var tts: TextToSpeech
    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseConnection()

    // --- VARIABLES DEL JUEGO ---
    private var fraseOriginal: String = ""
    private var palabrasCorrectas: List<String> = emptyList()

    // Listas para gestionar el estado actual de la pantalla
    private val palabrasEnBanco = mutableListOf<String>()    // Abajo
    private val palabrasEnRespuesta = mutableListOf<String>() // Arriba

    // Control de Rondas
    private var listaIdsFrases: List<String> = emptyList()
    private var indiceActual = 0
    private var aciertos = 0
    private var fallos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJuegoFrasesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)
        setupUI()
        iniciarJuego()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonConfirm.setOnClickListener { comprobarRespuesta() }
        binding.buttonNext.setOnClickListener { siguienteFrase() }
        binding.buttonJugarDeNuevo.setOnClickListener { finish() }

        // Botón de audio
        binding.fabPlaySound.setOnClickListener {
            if (fraseOriginal.isNotEmpty()) speak(fraseOriginal)
        }
    }

    private fun iniciarJuego() {
        binding.gameContentGroup.visibility = View.VISIBLE
        binding.resumenLayout.visibility = View.GONE
        aciertos = 0
        fallos = 0
        indiceActual = 0

        // Obtenemos IDs de frases (puedes filtrar por dificultad si quieres)
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("frases").get().await()
                if (!snapshot.isEmpty) {
                    listaIdsFrases = snapshot.documents.map { it.id }.shuffled()
                    cargarFrase(listaIdsFrases[indiceActual])
                } else {
                    Toast.makeText(this@JuegoFrasesActivity, "No hay frases disponibles", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@JuegoFrasesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarFrase(idFrase: String) {
        lifecycleScope.launch {
            val doc = db.collection("frases").document(idFrase).get().await()

            // Recogemos datos
            fraseOriginal = doc.getString("frase") ?: ""
            val urlImagen = doc.getString("urlImagen") ?: ""
            // Firebase guarda Arrays como List<Any>, casteamos
            palabrasCorrectas = (doc.get("palabras") as? List<String>) ?: fraseOriginal.split(" ")

            // Actualizar vista
            binding.imageViewPictogram.load(urlImagen)
            binding.progressBarGame.progress = indiceActual + 1
            binding.progressBarGame.max = listaIdsFrases.size

            // Preparar lógica de palabras
            palabrasEnRespuesta.clear()
            palabrasEnBanco.clear()
            palabrasEnBanco.addAll(palabrasCorrectas.shuffled()) // Desordenamos para el banco

            renderizarChips()

            // Reseteamos botones
            binding.buttonConfirm.visibility = View.INVISIBLE
            binding.buttonNext.visibility = View.INVISIBLE
        }
    }

    /**
     * Dibuja los Chips (botones de palabra) en la zona de arriba y abajo
     * según el contenido de las listas `palabrasEnRespuesta` y `palabrasEnBanco`.
     */
    private fun renderizarChips() {
        // 1. Limpiar contenedores
        binding.chipGroupRespuesta.removeAllViews()
        binding.chipGroupOpciones.removeAllViews()

        // 2. Dibujar zona RESPUESTA (Arriba)
        palabrasEnRespuesta.forEachIndexed { index, palabra ->
            val chip = crearChip(palabra, esRespuesta = true)
            chip.setOnClickListener {
                // AL TOCAR ARRIBA: Devuelve la palabra al banco
                palabrasEnRespuesta.removeAt(index)
                palabrasEnBanco.add(palabra)
                renderizarChips()
            }
            binding.chipGroupRespuesta.addView(chip)
        }

        // 3. Dibujar zona BANCO (Abajo)
        palabrasEnBanco.forEachIndexed { index, palabra ->
            val chip = crearChip(palabra, esRespuesta = false)
            chip.setOnClickListener {
                // AL TOCAR ABAJO: Sube la palabra a la respuesta
                palabrasEnBanco.removeAt(index)
                palabrasEnRespuesta.add(palabra)
                renderizarChips()
            }
            binding.chipGroupOpciones.addView(chip)
        }

        // 4. Habilitar botón confirmar si se han usado todas las palabras
        if (palabrasEnBanco.isEmpty() && palabrasEnRespuesta.isNotEmpty()) {
            binding.buttonConfirm.visibility = View.VISIBLE
        } else {
            binding.buttonConfirm.visibility = View.INVISIBLE
        }
    }

    private fun crearChip(texto: String, esRespuesta: Boolean): Chip {
        val chip = Chip(this)
        chip.text = texto
        chip.isCheckable = false
        chip.textSize = 18f

        if (esRespuesta) {
            // Estilo para chips en la zona de respuesta
            chip.setChipBackgroundColorResource(R.color.white)
            chip.chipStrokeWidth = 2f
            chip.setChipStrokeColorResource(R.color.black)
        } else {
            // Estilo para chips en el banco (opciones)
            chip.setChipBackgroundColorResource(R.color.Mustard) // O el color amarillo que usas
            chip.setTextColor(Color.BLACK)
        }
        return chip
    }

    private fun comprobarRespuesta() {
        // Construimos la frase del usuario uniendo las palabras
        val fraseUsuario = palabrasEnRespuesta.joinToString(" ")

        // Normalizamos strings (quitar mayusculas/puntos si quieres ser flexible)
        // Aquí comparamos exacto para enseñar gramática, o con ignoreCase
        if (fraseUsuario.equals(fraseOriginal, ignoreCase = true)) {
            // CORRECTO
            aciertos++
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
            pintarRespuesta(true)
            speak("¡Muy bien!")
        } else {
            // INCORRECTO
            fallos++
            Toast.makeText(this, "Incorrecto. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
            pintarRespuesta(false)
            // Opcional: speak("Inténtalo de nuevo")
        }

        binding.buttonConfirm.visibility = View.INVISIBLE
        binding.buttonNext.visibility = View.VISIBLE

        // Bloquear chips para que no muevan nada hasta pasar de ronda
        deshabilitarChips()
    }

    private fun pintarRespuesta(esCorrecto: Boolean) {
        val color = if (esCorrecto) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        for (i in 0 until binding.chipGroupRespuesta.childCount) {
            val chip = binding.chipGroupRespuesta.getChildAt(i) as Chip
            chip.setChipBackgroundColor(ColorStateList.valueOf(color))
            chip.setTextColor(Color.WHITE)
        }
    }

    private fun deshabilitarChips() {
        for (i in 0 until binding.chipGroupRespuesta.childCount) {
            binding.chipGroupRespuesta.getChildAt(i).isEnabled = false
        }
        for (i in 0 until binding.chipGroupOpciones.childCount) {
            binding.chipGroupOpciones.getChildAt(i).isEnabled = false
        }
    }

    private fun siguienteFrase() {
        indiceActual++
        if (indiceActual < listaIdsFrases.size) {
            cargarFrase(listaIdsFrases[indiceActual])
        } else {
            mostrarResumen()
        }
    }

    private fun mostrarResumen() {
        binding.gameContentGroup.visibility = View.GONE
        binding.resumenLayout.visibility = View.VISIBLE
        binding.textViewResumenAciertos.text = "Aciertos: $aciertos"
        // Guardar en BD si quieres (usando repository.saveStudentAttempt)
    }

    // --- TTS ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "ES")
        }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}