package com.example.edunova

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import com.example.edunova.databinding.JuegoRetoBinding
import com.example.edunova.databinding.SilabasBinding
import java.util.Locale
import com.google.android.material.appbar.MaterialToolbar
import kotlin.text.uppercase
import kotlin.text.uppercaseChar


class RetoActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: JuegoRetoBinding
    private lateinit var letterMap: Map<Char, TextView>
    private lateinit var tts: TextToSpeech
    private var indiceGrupoActual: Int = -1
    private var aciertos = 0
    private var fallos = 0
    private var palabraActual: String? = null





    private var abecedarioEspanol: MutableList<Char> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = JuegoRetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa el motor TTS aquí, pero el juego comenzará después en onInit
        tts = TextToSpeech(this, this)

        // Las inicializaciones rápidas se quedan aquí
        initializeLetterMap()
        inicializarAbecedario()

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener {
            // Crea un Intent para ir de MainActivity a RegisterActivity
            finish()
        }
        binding.botonConfirmar.setOnClickListener {
            val respuestaUsuario = binding.respuesta.text.toString().trim()
            if (palabraActual != null) {
                verificarRespuesta(respuestaUsuario)
            }

        }

        binding.fabPlaySoundSilabas.setOnClickListener {
            //reproducirSonido(binding.NombreImagen.text.toString())
        }
        binding.buttonJugarDeNuevo.setOnClickListener {
            //reiniciarActividad()
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
        binding.botonConfirmar.isEnabled = true
        avanzarAlSiguienteGrupo()
    }

    private fun avanzarAlSiguienteGrupo() {
        indiceGrupoActual++

        // La comprobación del final del juego ya no es necesaria aquí.
        // Solo avanzamos si no hemos llegado al final.
        if (indiceGrupoActual < 27) {
            val letraActual = abecedarioEspanol[indiceGrupoActual]
            // 1. Asignamos esa letra como la respuesta correcta que esperamos.
            palabraActual = letraActual.toString()


            // 2. Mostramos en la UI qué letra se está trabajando.
            // Puedes mostrar la letra directamente o un mensaje como "Letra C".
            //binding.TextoSilabas.text = "Adivina la letra: ${letraActual.uppercase()}" // Ejemplo

            // 3. Limpiamos el campo de texto para la nueva respuesta.
            binding.respuesta.text?.clear()

        } else {
            // El juego ha terminado.
            Toast.makeText(this, "¡Juego completado!", Toast.LENGTH_LONG).show()
            binding.botonConfirmar.isEnabled = false // Deshabilitar el botón de confirmar.
        }
    }

    private fun inicializarAbecedario(){
        //1. Generar la lista del abecedario estándar
        val abecedarioSinN = ('a'..'z').toMutableList()

        // 2. Insertar la "ñ" en la posición correcta
        // La "ñ" va después de la "n". Necesitamos encontrar el índice de la "n".
        val indiceDeLaN = abecedarioSinN.indexOf('n')
        if (indiceDeLaN != -1) {
            // Se inserta en la posición siguiente a la "n"
            abecedarioSinN.add(indiceDeLaN + 1, 'ñ')
        }

         abecedarioEspanol = abecedarioSinN

    }

    private fun verificarRespuesta(respuesta: String) {
        // 1. La respuesta correcta ya está guardada en `silabaActual`.
        val nombrePictograma = "a"

        // Salimos de la función si por alguna razón no hay una sílaba/letra actual.
        if (nombrePictograma == null) return

        // 2. Obtener la letra actual del abecedario usando el índice.
        val letraActual = abecedarioEspanol.getOrNull(indiceGrupoActual)
        val textViewDeLetra = if (letraActual != null) letterMap[letraActual.uppercaseChar()] else null

        // 3. Comparar la respuesta del usuario con la respuesta correcta.
        if (respuesta.equals(nombrePictograma, ignoreCase = true)) {
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
            aciertos++
            // Cambiamos el color del TextView correspondiente a la letra.
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_correcto)
        } else {
            Toast.makeText(this, "Incorrecto. La respuesta era '$nombrePictograma'", Toast.LENGTH_SHORT).show()
            fallos++
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.design_default_color_error)
        }

        // Importante: Después de verificar, avanzamos a la siguiente letra.
        avanzarAlSiguienteGrupo()
    }


    private fun initializeLetterMap() {
        letterMap = mapOf(

            // Vocales
            'A' to binding.A,
            'E' to binding.E,
            'I' to binding.I,
            'O' to binding.O,
            'U' to binding.U,

            //consonantes
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
            'Ñ' to binding.NEne, // Revisa que el ID sea correcto
            'P' to binding.P,
            'Q' to binding.Q,
            'R' to binding.letraR, // Revisa que el ID sea correcto
            'S' to binding.S,
            'T' to binding.T,
            'V' to binding.V,
            'W' to binding.W,
            'X' to binding.X,
            'Y' to binding.Y,
            'Z' to binding.Z
        )
    }






}