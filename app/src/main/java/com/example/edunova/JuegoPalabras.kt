package com.example.edunova

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale


class JuegoPalabras : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.juego_palabras)

        // Inicializamos TextToSpeech
        tts = TextToSpeech(this, this)

        val botonHablar = findViewById<Button>(R.id.botonSonido)
        botonHablar.setOnClickListener {
            reproducirTexto("Hola, este es un ejemplo de texto a voz en Kotlin.")
        }
    }

    // Se llama automáticamente cuando TTS está listo

    //Si queremos que se mantenga con una pronuciacion en concreto
    //tts.voice = tts.voices.find { it.name.contains("es-es-x-ana") }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Idioma no soportado
                println("Idioma no soportado")
            }
        }
    }

    // Función reutilizable
    private fun reproducirTexto(texto: String) {
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Liberar recursos al cerrar
    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    //Función que podremos usar más tarde para cambiar el texto de los botónes al necesario para
    //hacer referencia a la batería de palabras
    fun cambiarTextoBotones(texto1: String, texto2: String, texto3: String){
        // Vinculamos el botón con su ID en el XML
        val miBoton1: Button = findViewById(R.id.button3)
        val miBoton2: Button = findViewById(R.id.button4)
        val miBoton3: Button = findViewById(R.id.button5)

        // Cambiar el texto directamente
        miBoton1.text = texto1
        miBoton2.text = texto1
        miBoton3.text = texto1

    }
}
