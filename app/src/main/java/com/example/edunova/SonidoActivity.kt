package com.example.edunova

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale


class SonidoActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pruebasonido)

        // Inicializamos TextToSpeech
        tts = TextToSpeech(this, this)

        val botonHablar = findViewById<Button>(R.id.botonHablar)
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
}
