package com.example.edunova

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.edunova.databinding.ActivityLearnBinding
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.Locale

class JuegoPalabras : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var binding: ActivityLearnBinding

    // --- VARIABLES DEL JUEGO ---
    private var palabraCorrectaActual: String? = null
    private var aciertos = 0
    private var fallos = 0
    private var indice = 0
    private var listaDeIds: List<String> = emptyList()

    private var listaAciertos = mutableListOf<String>()
    private var listaFallos = mutableListOf<String>()

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val repository = FirebaseConnection()
    private var datosAlumno: Map<String, Any>? = null
    private var tiempoInicio: Long = 0
    private var idPictogramaActual: String = ""

    // VARIABLE NUEVA: Categoría seleccionada
    private var selectedCategory: String = "Aleatorio"

    private val detallesDelIntento = mutableListOf<Map<String, Any>>()

    private lateinit var soundPool: SoundPool
    private var sonidoAciertoId: Int = 0
    private var sonidoFalloId: Int = 0

    private val MINIMO_ACIERTOS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        // Recuperar categoría elegida
        selectedCategory = intent.getStringExtra("EXTRA_CATEGORY") ?: "Aleatorio"

        // Actualizar título
        binding.toolbar.title = if (selectedCategory == "Aleatorio") "Vocabulario (Aleatorio)" else "Categoría: $selectedCategory"

        configurarListeners()
        inicializarSoundPool()

        // 1. CARGAR DATOS Y LUEGO INICIAR (Igual que en Frases)
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getUserData(currentUser.uid) { data ->
                datosAlumno = data
                val school = datosAlumno?.get("school") as? String

                if (!school.isNullOrEmpty()) {
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

    private fun configurarListeners() {
        binding.toggleGroupOptions.addOnButtonCheckedListener { _, _, _ ->
            if (binding.toggleGroupOptions.checkedButtonId != View.NO_ID) {
                binding.buttonConfirm.visibility = View.VISIBLE
            } else {
                binding.buttonConfirm.visibility = View.INVISIBLE
            }
        }

        binding.buttonConfirm.setOnClickListener { comprobarRespuesta() }

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener { finish() }

        binding.buttonNext.setOnClickListener { avanzarSiguienteRonda() }

        binding.buttonJugarDeNuevo.setOnClickListener { reiniciarActividad() }

        binding.buttonSalir.setOnClickListener { finish() }
    }

    // --- LÓGICA DE INICIO MODIFICADA ---
    private fun iniciarJuego(school: String) {
        aciertos = 0
        fallos = 0
        indice = 0
        detallesDelIntento.clear()
        listaAciertos.clear()
        listaFallos.clear()
        tiempoInicio = System.currentTimeMillis()

        lifecycleScope.launch {
            // Obtenemos los IDs filtrados por escuela y categoría
            listaDeIds = getFilteredPictogramIds(school, selectedCategory)

            if (listaDeIds.isNotEmpty()) {
                cargarPictograma(listaDeIds[indice])
            } else {
                val msg = if(selectedCategory == "Aleatorio")
                    "No hay pictogramas en tu centro."
                else
                    "No hay pictogramas en la categoría '$selectedCategory'."
                Toast.makeText(this@JuegoPalabras, msg, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // --- NUEVA FUNCIÓN DE FILTRADO ---
    private suspend fun getFilteredPictogramIds(school: String, category: String): List<String> {
        return try {
            var query = db.collection("palabras")
                .whereEqualTo("school", school)

            // Si no es aleatorio, filtramos también por categoría
            if (category != "Aleatorio") {
                query = query.whereEqualTo("categoria", category)
            }

            val snapshot = query.get().await()

            // Barajamos y cogemos máximo 10
            snapshot.documents.map { it.id }.shuffled().take(10)

        } catch (e: Exception) {
            Log.e("JuegoPalabras", "Error obteniendo pictogramas", e)
            emptyList()
        }
    }

    private fun cargarPictograma(pictogramaId: String) {
        idPictogramaActual = pictogramaId
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val documentSnapshot = db.collection("palabras").document(pictogramaId).get().await()

                if (documentSnapshot.exists()) {
                    val imageUrl = documentSnapshot.getString("urlImagen")
                    val palabraCorrecta = documentSnapshot.getString("palabra")

                    if (!imageUrl.isNullOrEmpty() && !palabraCorrecta.isNullOrEmpty()) {
                        palabraCorrectaActual = palabraCorrecta
                        binding.imageViewPictogram.load(imageUrl) {
                            placeholder(android.R.drawable.ic_menu_gallery)
                        }

                        // Pasamos el school para buscar distractores del mismo centro
                        val school = datosAlumno?.get("school") as? String ?: ""
                        prepararBotones(palabraCorrecta, school)

                    } else {
                        avanzarSiguienteRonda()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@JuegoPalabras, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private suspend fun prepararBotones(palabraCorrecta: String, school: String) {
        val botones = mutableListOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)
        val botonCorrecto = botones.random()
        botonCorrecto.text = palabraCorrecta

        findViewById<FloatingActionButton>(R.id.fabPlaySound).setOnClickListener {
            reproducirTexto(palabraCorrecta)
        }

        botones.remove(botonCorrecto)
        // Buscamos distractores del MISMO CENTRO
        val distractores = getPalabrasAleatoriasIncorrectas(palabraCorrecta, 2, school)

        if (distractores.size >= 2) {
            botones[0].text = distractores[0]
            botones[1].text = distractores[1]
        } else {
            botones.forEach { it.text = "---" }
        }

        // Reset visual de botones
        resetearEstiloBotones()
    }

    private fun resetearEstiloBotones() {
        val botones = listOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)
        val colorFondo = ContextCompat.getColorStateList(this, R.color.option_button_background_color) // Asegúrate de tener este color o usa R.color.white
        val colorTexto = ContextCompat.getColorStateList(this, R.color.black)
        val colorBorde = ContextCompat.getColorStateList(this, R.color.option_button_stroke_color) // o un color gris

        for (boton in botones) {
            boton.isEnabled = true
            boton.backgroundTintList = colorFondo
            boton.strokeColor = colorBorde
            boton.setTextColor(colorTexto)
            boton.isChecked = false
        }
        binding.toggleGroupOptions.clearChecked()
    }

    // --- MODIFICADO: Distractores también del centro ---
    private suspend fun getPalabrasAleatoriasIncorrectas(excluir: String, cantidad: Int, school: String): List<String> {
        val distractores = mutableListOf<String>()

        try {
            // 1. INTENTO PRINCIPAL: Buscar palabras del mismo centro
            val snapshotSchool = db.collection("palabras")
                .whereEqualTo("school", school)
                .get().await() // Traemos todas las del centro (no suelen ser miles) y filtramos en memoria

            val palabrasSchool = snapshotSchool.documents
                .mapNotNull { it.getString("palabra") }
                .filter { it != excluir }
                .distinct()
                .shuffled()

            distractores.addAll(palabrasSchool.take(cantidad))

            // 2. RELLENO (FALLBACK): Si no hay suficientes, buscamos globales (o de otros centros)
            if (distractores.size < cantidad) {
                val faltan = cantidad - distractores.size

                // Traemos palabras generales (limitadas para no cargar toda la base de datos)
                val snapshotGlobal = db.collection("palabras")
                    .limit(50)
                    .get().await()

                val palabrasGlobales = snapshotGlobal.documents
                    .mapNotNull { it.getString("palabra") }
                    .filter { it != excluir && !distractores.contains(it) } // Que no sea la correcta ni ya esté añadida
                    .distinct()
                    .shuffled()

                distractores.addAll(palabrasGlobales.take(faltan))
            }

        } catch (e: Exception) {
            Log.e("JuegoPalabras", "Error buscando distractores", e)
        }

        // 3. SEGURIDAD FINAL: Si tras todo esto seguimos sin tener 2, rellenamos con texto genérico
        while (distractores.size < cantidad) {
            distractores.add("---") // Mejor que "Error"
        }

        return distractores
    }

    // ... (comprobacionVisual, comprobarRespuesta, avanzarSiguienteRonda... IGUAL QUE ANTES) ...
    // Solo asegúrate de que resetearEstadoRonda llame a resetearEstiloBotones()

    private fun comprobarRespuesta() {
        val idSeleccionado = binding.toggleGroupOptions.checkedButtonId
        if (idSeleccionado == View.NO_ID || palabraCorrectaActual == null) return

        val botonSeleccionado = findViewById<MaterialButton>(idSeleccionado)
        val respuestaUsuario = botonSeleccionado.text.toString()

        bloquearInteraccion(false)

        val esCorrecto = (respuestaUsuario == palabraCorrectaActual)

        detallesDelIntento.add(hashMapOf(
            "questionIndex" to indice,
            "pictogramId" to idPictogramaActual,
            "targetWord" to palabraCorrectaActual!!,
            "userResponse" to respuestaUsuario,
            "isCorrect" to esCorrecto,
            "timestamp" to System.currentTimeMillis()
        ))

        if (esCorrecto) {
            aciertos++
            palabraCorrectaActual?.let { listaAciertos.add(it) }
            soundPool.play(sonidoAciertoId, 1.0f, 1.0f, 1, 0, 1.0f)
            comprobacionVisual(botonSeleccionado, "#4CAF50")
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
        } else {
            fallos++
            palabraCorrectaActual?.let { listaFallos.add(it) }
            soundPool.play(sonidoFalloId, 1.0f, 1.0f, 1, 0, 1.0f)
            comprobacionVisual(botonSeleccionado, "#F44336")

            val botonCorrecto = listOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)
                .find { it.text == palabraCorrectaActual }
            botonCorrecto?.let { comprobacionVisual(it, "#4CAF50") }

            Toast.makeText(this, "La correcta era: $palabraCorrectaActual", Toast.LENGTH_SHORT).show()
        }

        binding.buttonConfirm.visibility = View.INVISIBLE
        binding.buttonNext.visibility = View.VISIBLE
    }

    private fun comprobacionVisual(boton: Button, colorHex: String) {
        boton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
        boton.setTextColor(Color.WHITE)
    }

    private fun avanzarSiguienteRonda() {
        indice++
        if (indice >= listaDeIds.size) {
            finalizarJuego()
        } else {
            resetearEstiloBotones()
            cargarPictograma(listaDeIds[indice])
            binding.buttonNext.visibility = View.INVISIBLE
            binding.buttonConfirm.visibility = View.INVISIBLE
        }
    }

    private fun bloquearInteraccion(habilitar: Boolean) {
        binding.buttonConfirm.isEnabled = habilitar
        binding.buttonOption1.isEnabled = habilitar
        binding.buttonOption2.isEnabled = habilitar
        binding.buttonOption3.isEnabled = habilitar
    }

    private fun finalizarJuego() {
        binding.gameContentGroup.visibility = View.GONE
        binding.resumenLayout.visibility = View.VISIBLE

        val tiempoTotalMs = System.currentTimeMillis() - tiempoInicio
        val segundosTotales = tiempoTotalMs / 1000
        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60
        val tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

        binding.textViewResumenAciertos.text = "Aciertos: $aciertos"
        binding.textViewResumenFallos.text = "Fallos: $fallos"
        binding.textViewResumenTiempo.text = "Tiempo: $tiempoFormateado"

        guardarResultadosEnBD()
    }

    private fun guardarResultadosEnBD() {
        val currentUser = repository.getCurrentUser() ?: return
        val studentUid = currentUser.uid
        val tiempoTotalMs = System.currentTimeMillis() - tiempoInicio
        val tiempoTotalSegundos = tiempoTotalMs / 1000

        val intentoData = hashMapOf(
            "studentUid" to studentUid,
            "studentName" to (datosAlumno?.get("displayName") ?: "Alumno"),
            "school" to (datosAlumno?.get("school") ?: "Sin Centro"),
            "classroom" to (datosAlumno?.get("classroom") ?: "Sin Clase"),
            "exerciseType" to "vocabulario",
            "category" to selectedCategory, // Guardamos la categoría jugada
            "timestamp" to System.currentTimeMillis(),
            "timeSpentSeconds" to tiempoTotalSegundos,
            "score" to aciertos,
            "totalQuestions" to listaDeIds.size,
            "status" to "completed",
            "details" to detallesDelIntento,
            "aciertos" to listaAciertos,
            "fallos" to listaFallos
        )

        repository.saveStudentAttempt(intentoData) { success ->
            if (success) Log.d("Juego", "Guardado OK")
        }

        if (aciertos >= MINIMO_ACIERTOS) {
            val progressUpdate = mapOf("completedPalabras" to true)
            db.collection("userProgress").document(studentUid)
                .set(progressUpdate, SetOptions.merge())
        }
    }

    private fun mostrarCargando(loading: Boolean) {
        binding.progressBarGame.visibility = if (loading) View.VISIBLE else View.GONE
        bloquearInteraccion(!loading)
    }

    private fun reiniciarActividad() {
        binding.resumenLayout.visibility = View.GONE
        binding.gameContentGroup.visibility = View.VISIBLE
        // Recargamos el school porque lo tenemos en memoria
        val school = datosAlumno?.get("school") as? String
        if (school != null) {
            iniciarJuego(school)
        } else {
            finish()
        }
    }

    private fun inicializarSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        sonidoAciertoId = soundPool.load(this, R.raw.sonido_correcto, 1)
        sonidoFalloId = soundPool.load(this, R.raw.sonido_incorrecto, 1)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale("es", "ES")
    }

    private fun reproducirTexto(texto: String) {
        if (::tts.isInitialized) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}