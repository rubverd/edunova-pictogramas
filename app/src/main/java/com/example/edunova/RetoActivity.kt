package com.example.edunova

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.edunova.databinding.JuegoRetoBinding
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class RetoActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: JuegoRetoBinding
    private lateinit var letterMap: Map<Char, TextView>
    private lateinit var tts: TextToSpeech
    private var indiceGrupoActual: Int = -1
    private var aciertos = 0
    private var fallos = 0
    private var palabraActual: String? = null

    // --- VARIABLES DE SEGUIMIENTO ---
    private var tiempoInicioJuego: Long = 0L
    private val repository = FirebaseConnection()
    private var datosAlumno: Map<String, Any>? = null

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val palabrasUsadasEnElRosco = mutableListOf<String>()
    private var abecedarioEspanol: MutableList<Char> = mutableListOf()

    // --- VARIABLES PARA SONIDOS ---
    private lateinit var soundPool: SoundPool
    private var sonidoAciertoId: Int = 0
    private var sonidoFalloId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = JuegoRetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        // 1. CARGAR DATOS DEL ALUMNO
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getUserData(currentUser.uid) { data ->
                datosAlumno = data
                Log.d("RetoActivity", "Datos alumno cargados: ${datosAlumno?.get("displayName")}")
            }
        }

        initializeLetterMap()
        inicializarAbecedario()
        inicializarSoundPool() // Iniciamos sonidos

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener { finish() }

        binding.botonConfirmar.setOnClickListener {
            val respuestaUsuario = binding.respuesta.text.toString().trim()
            if (palabraActual != null) {
                verificarRespuesta(respuestaUsuario)
            }
        }

        binding.fabPlaySoundSilabas.setOnClickListener {
            reproducirSonido(palabraActual.toString())
        }

        // --- BOTONES DEL RESUMEN ---
        binding.buttonJugarDeNuevo.setOnClickListener {
            reiniciarActividad()
        }

        binding.buttonSalir.setOnClickListener {
            finish()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "El idioma español no está soportado.", Toast.LENGTH_SHORT).show()
            }
            iniciarRecorrido()
        } else {
            Toast.makeText(this, "Error al inicializar el motor de voz.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reproducirSonido(texto: String) {
        if (::tts.isInitialized && texto.isNotBlank()) {
            if (tts.voice.locale.toLanguageTag().startsWith("es")) {
                tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun iniciarRecorrido() {
        indiceGrupoActual = -1
        binding.botonConfirmar.isEnabled = true
        // 2. MARCAR TIEMPO INICIO
        tiempoInicioJuego = System.currentTimeMillis()
        avanzarAlSiguienteGrupo()
    }

    private fun avanzarAlSiguienteGrupo() {
        indiceGrupoActual++

        if (indiceGrupoActual < 27) {
            val letraActual = abecedarioEspanol[indiceGrupoActual]

            lifecycleScope.launch {
                var documentoPalabra: DocumentSnapshot? = obtenerPalabraPorLetra(letraActual)

                if (documentoPalabra == null) {
                    documentoPalabra = obtenerPalabraQueContengaLetra(letraActual, palabrasUsadasEnElRosco)
                }

                if (documentoPalabra != null) {
                    val palabra = documentoPalabra.getString("palabra") ?: "Error"
                    val urlImagen = documentoPalabra.getString("urlImagen")

                    if (!urlImagen.isNullOrEmpty()) {
                        binding.imagenReto.visibility = View.VISIBLE
                        Glide.with(this@RetoActivity)
                            .load(urlImagen)
                            .into(binding.imagenReto)
                    } else {
                        binding.imagenReto.visibility = View.GONE
                    }

                    palabraActual = palabra
                    palabrasUsadasEnElRosco.add(palabra)
                    mostrarPista(letraActual, palabraActual.toString())
                    binding.respuesta.isEnabled = true
                    binding.botonConfirmar.isEnabled = true
                } else {
                    binding.imagenReto.visibility = View.GONE
                    Log.w("Rosco", "No hay palabra para la letra '$letraActual', saltando turno.")
                }
            }
            binding.respuesta.text?.clear()
        } else {
            Toast.makeText(this, "¡Juego completado!", Toast.LENGTH_LONG).show()
            binding.botonConfirmar.isEnabled = false
            finalizarReto()
        }
    }

    private fun inicializarAbecedario() {
        val abecedarioSinN = ('a'..'z').toMutableList()
        val indiceDeLaN = abecedarioSinN.indexOf('n')
        if (indiceDeLaN != -1) {
            abecedarioSinN.add(indiceDeLaN + 1, 'ñ')
        }
        abecedarioEspanol = abecedarioSinN
    }

    private fun verificarRespuesta(respuesta: String) {
        val nombrePictograma = palabraActual
        if (nombrePictograma == null) return

        val letraActual = abecedarioEspanol.getOrNull(indiceGrupoActual)
        val textViewDeLetra = if (letraActual != null) letterMap[letraActual.uppercaseChar()] else null

        if (respuesta.equals(nombrePictograma, ignoreCase = true)) {
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
            aciertos++
            soundPool.play(sonidoAciertoId, 1.0f, 1.0f, 1, 0, 1.0f)
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_correcto)
        } else {
            Toast.makeText(this, "Incorrecto. La respuesta era '$nombrePictograma'", Toast.LENGTH_SHORT).show()
            fallos++
            soundPool.play(sonidoFalloId, 1.0f, 1.0f, 1, 0, 1.0f)
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.design_default_color_error)
        }
        avanzarAlSiguienteGrupo()
    }

    private fun initializeLetterMap() {
        letterMap = mapOf(
            'A' to binding.A, 'E' to binding.E, 'I' to binding.I, 'O' to binding.O, 'U' to binding.U,
            'B' to binding.B, 'C' to binding.C, 'D' to binding.D, 'F' to binding.F, 'G' to binding.G,
            'H' to binding.H, 'J' to binding.J, 'K' to binding.K, 'L' to binding.L, 'M' to binding.M,
            'N' to binding.N, 'Ñ' to binding.NEne, 'P' to binding.P, 'Q' to binding.Q, 'R' to binding.letraR,
            'S' to binding.S, 'T' to binding.T, 'V' to binding.V, 'W' to binding.W, 'X' to binding.X,
            'Y' to binding.Y, 'Z' to binding.Z
        )
    }

    private suspend fun obtenerPalabraPorLetra(letra: Char): DocumentSnapshot? {
        val letraMayuscula = letra.uppercaseChar()
        try {
            val letraSiguiente = (letraMayuscula.code + 1).toChar().toString()
            val querySnapshot = db.collection("palabras")
                .whereGreaterThanOrEqualTo("palabra", letraMayuscula.toString())
                .whereLessThan("palabra", letraSiguiente)
                .get().await()

            if (querySnapshot.isEmpty) return null
            return querySnapshot.documents.random()
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun obtenerPalabraQueContengaLetra(letra: Char, palabrasExcluidas: List<String>): DocumentSnapshot? {
        val letraMayuscula = letra.uppercaseChar()
        try {
            val querySnapshot = db.collection("palabras").get().await()
            if (querySnapshot.isEmpty) return null

            val resultadosValidos = querySnapshot.documents.filter { doc ->
                val palabraActual = doc.getString("palabra") ?: ""
                val noExcluida = !palabrasExcluidas.contains(palabraActual)
                if (!noExcluida) return@filter false

                val silabas = doc.get("silabas") as? List<String>
                silabas?.any { it.uppercase().contains(letraMayuscula) } ?: false
            }

            if (resultadosValidos.isEmpty()) return null
            return resultadosValidos.random()
        } catch (e: Exception) {
            return null
        }
    }

    private fun finalizarReto() {
        binding.gameContentGroup.visibility = View.GONE
        binding.resumenLayout.visibility = View.VISIBLE

        val tiempoFinalJuego = System.currentTimeMillis()
        val tiempoTotalMs = tiempoFinalJuego - tiempoInicioJuego
        val segundosTotales = tiempoTotalMs / 1000

        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60
        val tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

        binding.textViewResumenAciertos.text = getString(R.string.texto_aciertos, aciertos)
        binding.textViewResumenFallos.text = getString(R.string.texto_fallos, fallos)
        binding.textViewResumenTiempo.text = getString(R.string.texto_tiempo, tiempoFormateado)

        // 3. GUARDAR RESULTADOS
        guardarResultadosEnBD(segundosTotales)
    }

    private fun guardarResultadosEnBD(segundosTotales: Long) {
        val currentUser = repository.getCurrentUser() ?: return

        val intentoData = hashMapOf(
            "studentUid" to currentUser.uid,
            "studentName" to (datosAlumno?.get("displayName") ?: "Alumno"),
            "school" to (datosAlumno?.get("school") ?: "Sin Centro"),
            "classroom" to (datosAlumno?.get("classroom") ?: "Sin Clase"),
            "exerciseType" to "reto_abecedario",
            "timestamp" to System.currentTimeMillis(),
            "timeSpentSeconds" to segundosTotales,
            "score" to aciertos,
            "totalQuestions" to 27,
            "status" to "completed"
        )

        repository.saveStudentAttempt(intentoData) { success ->
            if (success) Log.d("RetoActivity", "Progreso guardado correctamente")
        }
    }

    private fun mostrarPista(letra: Char, palabra: String) {
        val pistaArray = CharArray(palabra.length) { '_' }
        val posicionLetra = palabra.indexOf(letra, ignoreCase = true)
        if (posicionLetra != -1) {
            pistaArray[posicionLetra] = palabra[posicionLetra]
        }
        val pistaConEspacios = pistaArray.joinToString(separator = " ")
        binding.textoPista.visibility = View.VISIBLE
        binding.textoPista.text = pistaConEspacios
    }

    private fun reiniciarActividad() {
        // Resetear vista
        binding.resumenLayout.visibility = View.GONE
        binding.gameContentGroup.visibility = View.VISIBLE

        // Resetear colores de letras
        letterMap.values.forEach { textView ->
            textView.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gris_contraste) // Asegúrate de tener este color o usa otro gris
        }

        // Resetear lógica
        aciertos = 0
        fallos = 0
        palabrasUsadasEnElRosco.clear()
        binding.respuesta.text?.clear()
        palabraActual = null

        iniciarRecorrido()
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

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}