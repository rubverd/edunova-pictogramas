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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    // Instancias de Firebase
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- VARIABLES PARA LA BASE DE DATOS Y SEGUIMIENTO ---
    private val repository = FirebaseConnection()
    private var datosAlumno: Map<String, Any>? = null
    private var tiempoInicio: Long = 0
    private var idPictogramaActual: String = ""

    // Lista para guardar el detalle de cada respuesta
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

        // Cargar datos del alumno
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getUserData(currentUser.uid) { data ->
                datosAlumno = data
                Log.d("Juego", "Datos alumno cargados: ${datosAlumno?.get("displayName")}")
            }
        }

        configurarListeners()
        inicializarSoundPool()
        iniciarJuego()
    }

    private fun configurarListeners() {
        binding.toggleGroupOptions.addOnButtonCheckedListener { _, _, _ ->
            // Solo mostramos confirmar si hay algo seleccionado
            if (binding.toggleGroupOptions.checkedButtonId != View.NO_ID) {
                binding.buttonConfirm.visibility = View.VISIBLE
            } else {
                binding.buttonConfirm.visibility = View.INVISIBLE
            }
        }

        binding.buttonConfirm.setOnClickListener {
            comprobarRespuesta()
        }

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener {
            finish()
        }

        binding.buttonNext.setOnClickListener {
            // NO limpiamos aquí, lo hace resetearEstadoRonda en el orden correcto
            avanzarSiguienteRonda()
        }

        binding.buttonJugarDeNuevo.setOnClickListener {
            reiniciarActividad()
        }
    }

    private fun iniciarJuego() {
        aciertos = 0
        fallos = 0
        indice = 0

        detallesDelIntento.clear()
        tiempoInicio = System.currentTimeMillis()

        lifecycleScope.launch {
            listaDeIds = getRandomPictogramaIds(10)

            if (listaDeIds.isNotEmpty()) {
                cargarPictograma(listaDeIds[indice])
            } else {
                Toast.makeText(this@JuegoPalabras, "No hay pictogramas disponibles.", Toast.LENGTH_LONG).show()
                finish()
            }
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
                        binding.imageViewPictogram.load(imageUrl)

                        prepararBotones(palabraCorrecta)
                    } else {
                        Log.e("Juego", "Datos incompletos en pictograma $pictogramaId")
                        avanzarSiguienteRonda() // Saltamos si está roto
                    }
                }
            } catch (e: Exception) {
                Log.e("Juego", "Error cargando pictograma", e)
                Toast.makeText(this@JuegoPalabras, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private suspend fun prepararBotones(palabraCorrecta: String) {
        val botones = mutableListOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)
        val botonCorrecto = botones.random()
        botonCorrecto.text = palabraCorrecta

        // Configurar audio
        findViewById<FloatingActionButton>(R.id.fabPlaySound).setOnClickListener {
            reproducirTexto(palabraCorrecta)
        }

        botones.remove(botonCorrecto)
        val distractores = getPalabrasAleatoriasIncorrectas(palabraCorrecta, 2)

        if (distractores.size >= 2) {
            botones[0].text = distractores[0]
            botones[1].text = distractores[1]
        } else {
            botones.forEach { it.text = "---" }
        }
    }

    private fun comprobacionVisual(boton: Button, colorHex: String) {
        // Usamos backgroundTintList para no romper la forma del MaterialButton
        boton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
        boton.setTextColor(Color.WHITE)
    }

    private fun comprobarRespuesta() {
        val idSeleccionado = binding.toggleGroupOptions.checkedButtonId
        if (idSeleccionado == View.NO_ID || palabraCorrectaActual == null) return

        val botonSeleccionado = findViewById<MaterialButton>(idSeleccionado)
        val respuestaUsuario = botonSeleccionado.text.toString()

        // Bloquear botones
        bloquearInteraccion(false)

        val esCorrecto = (respuestaUsuario == palabraCorrectaActual)

        // Registro
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
            soundPool.play(sonidoAciertoId, 1.0f, 1.0f, 1, 0, 1.0f)
            comprobacionVisual(botonSeleccionado, "#4CAF50") // Verde
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
        } else {
            fallos++
            soundPool.play(sonidoFalloId, 1.0f, 1.0f, 1, 0, 1.0f)
            comprobacionVisual(botonSeleccionado, "#F44336") // Rojo

            // Marcar la correcta en verde
            val botonCorrecto = listOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)
                .find { it.text == palabraCorrectaActual }
            botonCorrecto?.let { comprobacionVisual(it, "#4CAF50") }

            Toast.makeText(this, "La correcta era: $palabraCorrectaActual", Toast.LENGTH_SHORT).show()
        }

        binding.buttonConfirm.visibility = View.INVISIBLE
        binding.buttonNext.visibility = View.VISIBLE
    }

    private fun avanzarSiguienteRonda() {
        indice++
        if (indice >= listaDeIds.size) {
            finalizarJuego()
        } else {
            resetearEstadoRonda()
            cargarPictograma(listaDeIds[indice])
        }
    }

    // --- FUNCIÓN CORREGIDA: ORDEN DE OPERACIONES ---
    private fun resetearEstadoRonda() {
        val botones = listOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)

        // 1. PRIMERO habilitamos los botones.
        // Si limpiamos la selección estando deshabilitados, el visual no se refresca bien.
        botones.forEach { it.isEnabled = true }

        // 2. SEGUNDO limpiamos la selección del grupo
        binding.toggleGroupOptions.clearChecked()

        // 3. TERCERO restauramos los colores originales
        val colorFondo = ContextCompat.getColorStateList(this, R.color.option_button_background_color)
        val colorTexto = ContextCompat.getColorStateList(this, R.color.black)
        val colorBorde = ContextCompat.getColorStateList(this, R.color.option_button_stroke_color)

        for (boton in botones) {
            boton.backgroundTintList = colorFondo
            boton.strokeColor = colorBorde
            boton.setTextColor(colorTexto)
            boton.isChecked = false
        }

        binding.buttonNext.visibility = View.INVISIBLE
        binding.buttonConfirm.visibility = View.INVISIBLE
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
        // 1. Calcula el tiempo transcurrido en milisegundos.
        val tiempoFinal = System.currentTimeMillis()
        val tiempoTotalMs = tiempoFinal - tiempoInicio

        // 2. Convierte los milisegundos a minutos y segundos.
        val segundosTotales = tiempoTotalMs / 1000
        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60

        // 3. Formatea el tiempo en un string "MM:SS".
        val tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

        binding.textViewResumenAciertos.text = "Aciertos: $aciertos"
        binding.textViewResumenFallos.text = "Fallos: $fallos"
        binding.textViewResumenTiempo.text = "Tiempo: $tiempoFormateado"

        guardarResultadosEnBD()
    }

    private fun guardarResultadosEnBD() {
        val currentUser = repository.getCurrentUser() ?: return
        val studentUid = currentUser.uid

        // Calculamos tiempo total en SEGUNDOS
        val tiempoTotalMs = System.currentTimeMillis() - tiempoInicio
        val tiempoTotalSegundos = tiempoTotalMs / 1000

        val intentoData = hashMapOf(
            "studentUid" to currentUser.uid,
            "studentName" to (datosAlumno?.get("displayName") ?: "Alumno"),
            "school" to (datosAlumno?.get("school") ?: "Sin Centro"),
            "classroom" to (datosAlumno?.get("classroom") ?: "Sin Clase"),
            "exerciseType" to "vocabulario",

            // IMPORTANTE: Campo 'timestamp' genérico para ordenar y mostrar fecha
            "timestamp" to System.currentTimeMillis(),

            // Guardamos cuánto tardó
            "timeSpentSeconds" to tiempoTotalSegundos,

            "score" to aciertos,
            "totalQuestions" to listaDeIds.size,
            "status" to "completed",
            "details" to detallesDelIntento
        )

        repository.saveStudentAttempt(intentoData) { success ->
            if (success) Log.d("Juego", "Guardado OK con fecha y tiempo")
        }
        if (aciertos >= MINIMO_ACIERTOS) {
            val progressUpdate = mapOf("completedPalabras" to true)
            db.collection("userProgress").document(studentUid)
                .set(progressUpdate, SetOptions.merge())
                .addOnSuccessListener { Log.d("Juego", "Progreso actualizado correctamente") }
        }
    }

    private fun mostrarCargando(loading: Boolean) {
        binding.progressBarGame.visibility = if (loading) View.VISIBLE else View.GONE
        // Si estamos cargando, bloqueamos. Si no, desbloqueamos.
        bloquearInteraccion(!loading)
    }

    private fun reiniciarActividad() {
        binding.resumenLayout.visibility = View.GONE
        binding.gameContentGroup.visibility = View.VISIBLE
        iniciarJuego()
    }

    // --- HELPERS FIRESTORE ---
    private suspend fun getRandomPictogramaIds(count: Int): List<String> {
        return try {
            val snapshot = db.collection("palabras").get().await()
            snapshot.documents.map { it.id }.shuffled().take(count)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getPalabrasAleatoriasIncorrectas(excluir: String, cantidad: Int): List<String> {
        return try {
            val randomId = db.collection("palabras").document().id

            val snapshot = db.collection("palabras")
                .whereNotEqualTo("palabra", excluir)
                .limit((cantidad + 10).toLong())
                .get().await()

            val palabras = snapshot.documents.mapNotNull { it.getString("palabra") }
                .filter { it != excluir }
                .distinct()
                .shuffled()

            palabras.take(cantidad)
        } catch (e: Exception) {
            listOf("Error", "Error")
        }
    }

    // --- TTS ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale("es", "ES"))
        }
    }

    private fun reproducirTexto(texto: String) {
        if (::tts.isInitialized) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
    private fun inicializarSoundPool() {
        // Define los atributos de audio para el juego.
        val audioAttributes = AudioAttributes.Builder()
            // --- INICIO DE LA CORRECCIÓN ---
            // Usa la clase del paquete android.media, no androidx.media3
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            // --- FIN DE LA CORRECCIÓN ---
            .build()

        // Construye el SoundPool
        soundPool = SoundPool.Builder()
            .setMaxStreams(2) // Podemos reproducir 2 sonidos a la vez
            .setAudioAttributes(audioAttributes)
            .build()

        // Carga los sonidos desde la carpeta 'raw' y guarda sus IDs.
        // El '1' es la prioridad, pero no es muy relevante en este caso.
        sonidoAciertoId = soundPool.load(this, R.raw.sonido_correcto, 1)
        sonidoFalloId = soundPool.load(this, R.raw.sonido_incorrecto, 1)
    }

}