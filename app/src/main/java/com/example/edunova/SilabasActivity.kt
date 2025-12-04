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
import com.example.edunova.databinding.SilabasBinding
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.appbar.MaterialToolbar
import java.util.Locale

class SilabasActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: SilabasBinding
    private lateinit var letterMap: Map<Char, TextView>
    private lateinit var tts: TextToSpeech

    // --- VARIABLES DE SEGUIMIENTO ---
    private val repository = FirebaseConnection()
    private var datosAlumno: Map<String, Any>? = null
    private var tiempoInicioJuego: Long = 0L

    private var listaAciertos = mutableListOf<String>()

    private var listaFallos = mutableListOf<String>()

    private val gruposDeSilabasOrdenados: List<List<String>> by lazy {
        val silabas = listOf(
            "ba", "be", "bi", "bo", "bu", "ca", "ce", "ci", "co", "cu",
            "da", "de", "di", "do", "du", "fa", "fe", "fi", "fo", "fu",
            "ga", "ge", "gi", "go", "gu", "gui", "gue", "ha", "he", "hi", "ho", "hu",
            "ja", "je", "ji", "jo", "ju", "ka", "ke", "ki", "ko", "ku",
            "la", "le", "li", "lo", "lu", "ma", "me", "mi", "mo", "mu",
            "na", "ne", "ni", "no", "nu", "ña", "ñe", "ñi", "ño", "ñu", "pa", "pe", "pi", "po", "pu",
            "que", "qui", "ra", "re", "ri", "ro", "ru", "sa", "se", "si", "so", "su",
            "ta", "te", "ti", "to", "tu", "va", "ve", "vi", "vo", "vu",
            "wa", "we", "wi", "wo", "wu", "xa", "xe", "xi", "xo", "xu",
            "ya", "ye", "yi", "yo", "yu", "za", "ze", "zi", "zo", "zu"
        )

        val collator = java.text.Collator.getInstance(Locale("es", "ES"))
        collator.strength = java.text.Collator.PRIMARY
        val silabasOrdenadas = silabas.sortedWith(compareBy(collator) { it })
        silabasOrdenadas.groupBy { it.first() }.values.toList()
    }

    private var silabaActual: String? = null
    private var indiceGrupoActual: Int = -1

    private var aciertos = 0
    private var fallos = 0

    // --- SONIDOS ---
    private lateinit var soundPool: SoundPool
    private var sonidoAciertoId: Int = 0
    private var sonidoFalloId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SilabasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        // Cargar datos del alumno
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getUserData(currentUser.uid) { data ->
                datosAlumno = data
            }
        }

        initializeLetterMap()
        inicializarSoundPool()

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener { finish() }

        binding.buttonOption3.setOnClickListener {
            val respuestaUsuario = binding.respuesta.text.toString().trim()
            if (silabaActual != null) {
                verificarRespuesta(respuestaUsuario)
            }
        }

        // --- LÓGICA DEL BOTÓN DE ESCUCHAR ---
        binding.fabPlaySoundSilabas.setOnClickListener {
            // 1. Reproducimos usando la variable 'silabaActual' (memoria), NO el texto visible.
            silabaActual?.let { silaba ->
                reproducirSonido(silaba)
            }

            // 2. Borramos el texto para que "desaparezca" visualmente como pediste.
            binding.TextoSilabas.text = " "

            // 3. Habilitamos la escritura
            binding.respuesta.isEnabled = true
        }

        // --- LISTENERS BOTONES FINALES ---
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
        binding.buttonOption3.isEnabled = true
        // Bloqueamos escritura al inicio
        binding.respuesta.isEnabled = false
        tiempoInicioJuego = System.currentTimeMillis()
        avanzarAlSiguienteGrupo()
    }

    private fun avanzarAlSiguienteGrupo() {
        indiceGrupoActual++
        if (indiceGrupoActual < gruposDeSilabasOrdenados.size) {
            val grupoActual = gruposDeSilabasOrdenados[indiceGrupoActual]
            silabaActual = grupoActual.randomOrNull()

            // Mostramos la sílaba nueva como pista inicial
            binding.TextoSilabas.text = silabaActual ?: "Error"

            // Reseteamos el campo de texto y lo bloqueamos
            binding.respuesta.text?.clear()
            binding.respuesta.isEnabled = false
        }
    }

    private fun verificarRespuesta(respuesta: String) {
        val primeraSilabaDelGrupo = gruposDeSilabasOrdenados.getOrNull(indiceGrupoActual)?.firstOrNull()
        val letra = primeraSilabaDelGrupo?.firstOrNull()
        val textViewDeLetra = if (letra != null) letterMap[letra.uppercaseChar()] else null

        if (respuesta.equals(silabaActual, ignoreCase = true)) {
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
            soundPool.play(sonidoAciertoId, 1.0f, 1.0f, 1, 0, 1.0f)
            aciertos++
            silabaActual?.let { palabra ->
                listaAciertos.add(palabra)
            }
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_correcto)
        } else {
            Toast.makeText(this, "Incorrecto. La sílaba era '$silabaActual'", Toast.LENGTH_SHORT).show()
            soundPool.play(sonidoFalloId, 1.0f, 1.0f, 1, 0, 1.0f)
            fallos++
            silabaActual?.let { palabra ->
                listaFallos.add(palabra)
            }
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.design_default_color_error)
        }

        binding.respuesta.text?.clear()
        binding.respuesta.isEnabled = false

        if (indiceGrupoActual >= gruposDeSilabasOrdenados.size - 1) {
            finalizarJuego()
        } else {
            avanzarAlSiguienteGrupo()
        }
    }

    private fun initializeLetterMap() {
        letterMap = mapOf(
            'B' to binding.B, 'C' to binding.C, 'D' to binding.D, 'F' to binding.F,
            'G' to binding.G, 'H' to binding.H, 'J' to binding.J, 'K' to binding.K,
            'L' to binding.L, 'M' to binding.M, 'N' to binding.N, 'Ñ' to binding.NEne,
            'P' to binding.P, 'Q' to binding.Q, 'R' to binding.letraR, 'S' to binding.S,
            'T' to binding.T, 'V' to binding.V, 'W' to binding.W, 'X' to binding.X,
            'Y' to binding.Y, 'Z' to binding.Z
        )
    }

    private fun finalizarJuego() {
        binding.gameContentGroup.visibility = View.GONE
        binding.resumenLayout.visibility = View.VISIBLE

        val tiempoFinalJuego = System.currentTimeMillis()
        val tiempoTotalMs = tiempoFinalJuego - tiempoInicioJuego
        val segundosTotales = tiempoTotalMs / 1000

        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60
        val tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

        binding.textViewResumenAciertos.text = "Aciertos: $aciertos"
        binding.textViewResumenFallos.text = "Fallos: $fallos"
        binding.textViewResumenTiempo.text = "Tiempo: $tiempoFormateado"

        guardarResultadosEnBD(segundosTotales)
    }

    private fun guardarResultadosEnBD(segundosTotales: Long) {
        val currentUser = repository.getCurrentUser() ?: return

        val intentoData = hashMapOf(
            "studentUid" to currentUser.uid,
            "studentName" to (datosAlumno?.get("displayName") ?: "Alumno"),
            "school" to (datosAlumno?.get("school") ?: "Sin Centro"),
            "classroom" to (datosAlumno?.get("classroom") ?: "Sin Clase"),
            "exerciseType" to "silabas",
            "timestamp" to System.currentTimeMillis(),
            "timeSpentSeconds" to segundosTotales,
            "score" to aciertos,
            "totalQuestions" to gruposDeSilabasOrdenados.size,
            "status" to "completed",
            "aciertos" to listaAciertos,
            "fallos" to listaFallos
        )

        repository.saveStudentAttempt(intentoData) { success ->
            if (success) Log.d("SilabasActivity", "Progreso guardado correctamente")
        }
    }

    private fun reiniciarActividad() {
        binding.resumenLayout.visibility = View.GONE
        binding.gameContentGroup.visibility = View.VISIBLE

        letterMap.values.forEach { textView ->
            textView.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gris_contraste)
        }

        aciertos = 0
        fallos = 0
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
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}