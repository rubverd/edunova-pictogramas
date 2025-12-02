package com.example.edunova

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioAttributes // Importado
import android.media.SoundPool // Importado
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private val palabrasEnBanco = mutableListOf<String>()
    private val palabrasEnRespuesta = mutableListOf<String>()

    private var listaIdsFrases: List<String> = emptyList()
    private var indiceActual = 0
    private var aciertos = 0
    private var fallos = 0

    private var tiempoInicio: Long = 0
    private var datosAlumno: Map<String, Any>? = null

    // --- VARIABLES PARA SONIDOS (SoundPool) ---
    private lateinit var soundPool: SoundPool
    private var sonidoAciertoId: Int = 0
    private var sonidoFalloId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJuegoFrasesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)
        setupUI()

        // Inicializamos los sonidos aquí
        inicializarSoundPool()

        // 1. CARGAR DATOS DEL ALUMNO Y LUEGO INICIAR
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getUserData(currentUser.uid) { data ->
                datosAlumno = data
                val school = datosAlumno?.get("school") as? String

                if (!school.isNullOrEmpty()) {
                    Log.d("JuegoFrases", "Alumno del centro: $school")
                    // INICIAMOS EL JUEGO SOLO CUANDO TENEMOS EL CENTRO
                    iniciarJuego(school)
                } else {
                    Toast.makeText(this, "Error: No tienes centro asignado", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonConfirm.setOnClickListener { comprobarRespuesta() }
        binding.buttonNext.setOnClickListener { siguienteFrase() }
        binding.buttonJugarDeNuevo.setOnClickListener { finish() }

        binding.fabPlaySound.setOnClickListener {
            if (fraseOriginal.isNotEmpty()) speak(fraseOriginal)
        }
    }

    // --- MÉTODO PARA INICIALIZAR SONIDOS (Igual que en SilabasActivity) ---
    private fun inicializarSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        // Carga los sonidos desde la carpeta 'raw'
        sonidoAciertoId = soundPool.load(this, R.raw.sonido_correcto, 1)
        sonidoFalloId = soundPool.load(this, R.raw.sonido_incorrecto, 1)
    }

    private fun iniciarJuego(school: String) {
        binding.gameContentGroup.visibility = View.VISIBLE
        binding.resumenLayout.visibility = View.GONE
        aciertos = 0
        fallos = 0
        indiceActual = 0

        tiempoInicio = System.currentTimeMillis()

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("frases")
                    .whereEqualTo("school", school)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    listaIdsFrases = snapshot.documents.map { it.id }.shuffled()
                    cargarFrase(listaIdsFrases[indiceActual])
                } else {
                    Toast.makeText(this@JuegoFrasesActivity, "No hay frases en tu centro ($school)", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("JuegoFrases", "Error cargando frases", e)
                Toast.makeText(this@JuegoFrasesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarFrase(idFrase: String) {
        lifecycleScope.launch {
            val doc = db.collection("frases").document(idFrase).get().await()

            fraseOriginal = doc.getString("frase") ?: ""
            val urlImagen = doc.getString("urlImagen") ?: ""
            palabrasCorrectas = (doc.get("palabras") as? List<String>) ?: fraseOriginal.split(" ")

            binding.imageViewPictogram.load(urlImagen)
            binding.progressBarGame.progress = indiceActual + 1
            binding.progressBarGame.max = listaIdsFrases.size

            palabrasEnRespuesta.clear()
            palabrasEnBanco.clear()
            palabrasEnBanco.addAll(palabrasCorrectas.shuffled())

            renderizarChips()

            binding.buttonConfirm.visibility = View.INVISIBLE
            binding.buttonNext.visibility = View.INVISIBLE
        }
    }

    private fun renderizarChips() {
        binding.chipGroupRespuesta.removeAllViews()
        binding.chipGroupOpciones.removeAllViews()

        palabrasEnRespuesta.forEachIndexed { index, palabra ->
            val chip = crearChip(palabra, esRespuesta = true)
            chip.setOnClickListener {
                palabrasEnRespuesta.removeAt(index)
                palabrasEnBanco.add(palabra)
                renderizarChips()
            }
            binding.chipGroupRespuesta.addView(chip)
        }

        palabrasEnBanco.forEachIndexed { index, palabra ->
            val chip = crearChip(palabra, esRespuesta = false)
            chip.setOnClickListener {
                palabrasEnBanco.removeAt(index)
                palabrasEnRespuesta.add(palabra)
                renderizarChips()
            }
            binding.chipGroupOpciones.addView(chip)
        }

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
            chip.setChipBackgroundColorResource(R.color.white)
            chip.chipStrokeWidth = 2f
            chip.setChipStrokeColorResource(R.color.black)
        } else {
            chip.setChipBackgroundColorResource(R.color.Mustard)
            chip.setTextColor(Color.BLACK)
        }
        return chip
    }

    private fun comprobarRespuesta() {
        val fraseUsuario = palabrasEnRespuesta.joinToString(" ")

        if (fraseUsuario.equals(fraseOriginal, ignoreCase = true)) {
            // ACIERTO
            aciertos++
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
            pintarRespuesta(true)
            speak("¡Muy bien!")

            // --- REPRODUCIR SONIDO ACIERTO ---
            soundPool.play(sonidoAciertoId, 1.0f, 1.0f, 1, 0, 1.0f)
            // ---------------------------------

        } else {
            // FALLO
            fallos++
            Toast.makeText(this, "Incorrecto. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
            pintarRespuesta(false)

            // --- REPRODUCIR SONIDO FALLO ---
            soundPool.play(sonidoFalloId, 1.0f, 1.0f, 1, 0, 1.0f)
            // -------------------------------
        }

        binding.buttonConfirm.visibility = View.INVISIBLE
        binding.buttonNext.visibility = View.VISIBLE
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

        val tiempoTotalMs = System.currentTimeMillis() - tiempoInicio
        val segundosTotales = tiempoTotalMs / 1000

        binding.textViewResumenAciertos.text = "Aciertos: $aciertos"
        guardarResultadosEnBD(segundosTotales)
    }

    private fun guardarResultadosEnBD(segundosTotales: Long) {
        val currentUser = repository.getCurrentUser() ?: return

        val intentoData = hashMapOf(
            "studentUid" to currentUser.uid,
            "studentName" to (datosAlumno?.get("displayName") ?: "Alumno"),
            "school" to (datosAlumno?.get("school") ?: "Sin Centro"),
            "classroom" to (datosAlumno?.get("classroom") ?: "Sin Clase"),
            "exerciseType" to "frases",
            "timestamp" to System.currentTimeMillis(),
            "timeSpentSeconds" to segundosTotales,
            "score" to aciertos,
            "totalQuestions" to listaIdsFrases.size,
            "status" to "completed"
        )

        repository.saveStudentAttempt(intentoData) { success ->
            if (success) Log.d("JuegoFrases", "Progreso guardado correctamente")
        }
    }

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