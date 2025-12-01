package com.example.edunova

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

import com.example.edunova.databinding.SilabasBinding
import java.util.Locale
import com.google.android.material.appbar.MaterialToolbar


class SilabasActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: SilabasBinding
    private lateinit var letterMap: Map<Char, TextView>
    private lateinit var tts: TextToSpeech

    private var tiempoInicioJuego: Long = 0L

    // Esta parte está bien con by lazy, ya que la carga es diferida.
// En SilabasActivity.kt

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

        // --- LÓGICA DE ORDENAMIENTO CORREGIDA ---
        // 1. Obtener un Collator para el idioma español.
        val collator = java.text.Collator.getInstance(Locale("es", "ES"))
        collator.strength = java.text.Collator.PRIMARY // Ignora mayúsculas/minúsculas y acentos si fuera necesario

        // 2. Ordenar la lista de sílabas usando el Collator.
        val silabasOrdenadas = silabas.sortedWith(compareBy(collator) { it })

        // 3. Agrupar por la primera letra y devolver la lista de grupos.
        silabasOrdenadas.groupBy { it.first() }.values.toList()
    }

    private var silabaActual: String? = null
    private var indiceGrupoActual: Int = -1

    private var aciertos = 0
    private var fallos = 0

    // --- INICIO: VARIABLES PARA SONIDOS ---
    private lateinit var soundPool: SoundPool
    private var sonidoAciertoId: Int = 0
    private var sonidoFalloId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SilabasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa el motor TTS aquí, pero el juego comenzará después en onInit
        tts = TextToSpeech(this, this)

        // Las inicializaciones rápidas se quedan aquí
        initializeLetterMap()

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener {
            // Crea un Intent para ir de MainActivity a RegisterActivity
            finish()
        }

        binding.buttonOption3.setOnClickListener {
            val respuestaUsuario = binding.respuesta.text.toString().trim()
            if (silabaActual != null) {
                verificarRespuesta(respuestaUsuario)
            }

        }

        binding.fabPlaySoundSilabas.setOnClickListener {
            reproducirSonido(binding.TextoSilabas.text.toString())
            binding.TextoSilabas.text = " "
            binding.respuesta.isEnabled = true
        }
        inicializarSoundPool()

        binding.buttonJugarDeNuevo.setOnClickListener {
            reiniciarActividad()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "El idioma español no está soportado.", Toast.LENGTH_SHORT).show()
            }

            // ¡PUNTO CLAVE! Inicia el juego aquí, después de que TTS esté listo.
            // Esto asegura que la inicialización de TTS no bloquee el inicio.
            iniciarRecorrido()

        } else {
            Toast.makeText(this, "Error al inicializar el motor de voz.", Toast.LENGTH_SHORT).show()
            // Opcional: podrías deshabilitar las funciones de sonido si TTS falla
        }
    }




    private fun reproducirSonido(texto: String) {
        // La condición "::tts.isInitialized" ya asegura que TTS no se usará antes de estar listo.
        // La comprobación del idioma se hace de forma más segura.
        if (::tts.isInitialized && texto.isNotBlank()) {
            // Comprobamos si el idioma actual del motor de voz es español.
            // tts.voice.locale es la forma correcta y segura de hacerlo.
            if (tts.voice.locale.toLanguageTag().startsWith("es")) {
                tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                // Opcional: Informar al usuario que el sonido no se puede reproducir
                // si el idioma no es el correcto. Esto puede ayudar a depurar.
                // Toast.makeText(this, "El motor de voz no está en español.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun iniciarRecorrido() {
        indiceGrupoActual = -1
        // Asegúrate de que el botón esté habilitado al iniciar/reiniciar
        binding.buttonOption3.isEnabled = true

        tiempoInicioJuego = System.currentTimeMillis()
        avanzarAlSiguienteGrupo()
    }

    private fun avanzarAlSiguienteGrupo() {
        indiceGrupoActual++
        // La comprobación del final del juego ya no es necesaria aquí.
        // Solo avanzamos si no hemos llegado al final.
        if (indiceGrupoActual < gruposDeSilabasOrdenados.size) {
            val grupoActual = gruposDeSilabasOrdenados[indiceGrupoActual]
            silabaActual = grupoActual.randomOrNull()
            binding.TextoSilabas.text = silabaActual ?: "Error"
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
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_correcto)
        } else {
            Toast.makeText(this, "Incorrecto. La sílaba era '$silabaActual'", Toast.LENGTH_SHORT).show()
            soundPool.play(sonidoFalloId, 1.0f, 1.0f, 1, 0, 1.0f)
            fallos++
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.design_default_color_error)
        }

        binding.respuesta.text?.clear()
        binding.respuesta.isEnabled = false

        // --- LÓGICA DE FINALIZACIÓN MODIFICADA ---
        if (indiceGrupoActual >= gruposDeSilabasOrdenados.size - 1) {
            finalizarJuego() // Muestra la pantalla de resumen
        } else {
            avanzarAlSiguienteGrupo() // Si no es el final, avanza
        }
    }


    private fun initializeLetterMap() {
        letterMap = mapOf(
            'B' to binding.B,
            'C' to binding.C,
            'D' to binding.D,
            'F' to binding.F,
            'G' to binding.G,
            'H' to binding.H,
            'J' to binding.J,
            'K' to binding.K,
            'L' to binding.L,
            'M' to binding.M,
            'N' to binding.N,
            'Ñ' to binding.NEne,
            'P' to binding.P,
            'Q' to binding.Q,
            'R' to binding.letraR,
            'S' to binding.S,
            'T' to binding.T,
            'V' to binding.V,
            'W' to binding.W,
            'X' to binding.X,
            'Y' to binding.Y,
            'Z' to binding.Z
        )
    }

    override fun onDestroy() {
        // No olvides liberar los recursos de TTS cuando la actividad se destruya
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }


    private fun finalizarJuego() {
        // Ocultar vistas del juego usando el Group que definimos en el XML
        binding.gameContentGroup.visibility = View.GONE

        // Mostrar vistas del resumen
        binding.resumenLayout.visibility = View.VISIBLE


        // PASO 3.1: Calcula el tiempo transcurrido
        val tiempoFinalJuego = System.currentTimeMillis()
        val tiempoTotalMs = tiempoFinalJuego - tiempoInicioJuego // Tiempo en milisegundos
        val segundosTotales = tiempoTotalMs / 1000

        // PASO 3.2: Formatea el tiempo a minutos y segundos
        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60
        val tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

        // --- LÍNEAS CORREGIDAS ---
        // Usamos los recursos de string correctos que contienen el formato "%d"
        binding.textViewResumenAciertos.text = getString(R.string.texto_aciertos, aciertos)
        binding.textViewResumenFallos.text = getString(R.string.texto_fallos, fallos)
        binding.textViewResumenTiempo.text = getString(R.string.texto_tiempo, tiempoFormateado)
        4
    }





    private fun reiniciarActividad() {
        // Ocultar vistas del resumen
        binding.resumenLayout.visibility = View.GONE

        // Mostrar vistas del juego
        binding.gameContentGroup.visibility = View.VISIBLE


        // 1. Reiniciar los colores de fondo de todas las letras
        letterMap.values.forEach { textView ->
            textView.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gris_contraste)
        }

        // 2. Reiniciar los contadores y el recorrido
        aciertos = 0
        fallos = 0
        iniciarRecorrido()
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
