package com.example.edunova

import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.w3c.dom.Text
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
    fun cambiarTextoBotones(texto1: String, texto2: String, texto3: String,textoCorrecto:String){
        // Vinculamos el botón con su ID en el XML
        val miBoton1: Button = findViewById(R.id.button3)
        val miBoton2: Button = findViewById(R.id.button4)
        val miBoton3: Button = findViewById(R.id.button5)

        // Cambiar el texto directamente
        miBoton1.text = texto1
        miBoton2.text = texto1
        miBoton3.text = texto1
        //Llamar a un metodo que devuelva el resultado correcto cuando se puls el bóton
        //seguramente haya que usar otra vista y ocultarla
        if(texto1==textoCorrecto) {
            //correcto
            miBoton1.setOnClickListener {respuestaCorrecta(miBoton1)}
            //devolver resultado incorrecto
            miBoton2.setOnClickListener{respuestaIncorrecta(miBoton2)}
            miBoton3.setOnClickListener{respuestaIncorrecta(miBoton3)}
        }
        if(texto2==textoCorrecto) {
            //correcto
            miBoton2.setOnClickListener {respuestaCorrecta(miBoton2)}
            //devolver resultado incorrecto
            miBoton1.setOnClickListener{respuestaIncorrecta(miBoton1)}
            miBoton3.setOnClickListener{respuestaIncorrecta(miBoton3)}
        }
        if(texto3==textoCorrecto) {
            //correcto
            miBoton3.setOnClickListener {respuestaCorrecta(miBoton3)}
            //devolver resultado incorrecto
            miBoton1.setOnClickListener{respuestaIncorrecta(miBoton1)}
            miBoton2.setOnClickListener{respuestaIncorrecta(miBoton2)}
        }

    }
    //TODO: Ahora mismo no se usa el atributo de boton pero se podría llegar a usar para hacer
    //seguimiento de los resultados del usuario más adelante
    fun respuestaCorrecta(boton: Button ){
        val text: TextView= findViewById(R.id.ResultadoJuego)
        text.visibility = View.VISIBLE
        text.text= "Respuesta correcta"
        text.setBackgroundColor(Color.parseColor("#4CAF50"))
        text.alpha = 0f
        text.animate().alpha(1f).setDuration(300).start()

    }
    fun respuestaIncorrecta(boton: Button){
        val text: TextView= findViewById(R.id.ResultadoJuego)
        text.visibility = View.VISIBLE
        text.text= "Respuesta incorrecta"
        text.setBackgroundColor(Color.parseColor("#E78D8D"))
        text.alpha = 0f
        text.animate().alpha(1f).setDuration(300).start()

    }

}
