package com.example.edunova

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityMainBinding
import com.example.edunova.databinding.JuegoPalabrasBinding
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore


class JuegoPalabras : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var binding: JuegoPalabrasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)

        binding = JuegoPalabrasBinding.inflate(layoutInflater)

        // Inicializamos TextToSpeech
        tts = TextToSpeech(this, this)

        val botonHablar = findViewById<Button>(R.id.fabPlaySound)
        botonHablar.setOnClickListener {
            reproducirTexto("Hola, este es un ejemplo de texto a voz en Kotlin.")
        }

        //val botonVolver = findViewById< ImageButton>(R.id.returnButton)
        //botonVolver.setOnClickListener {
            // Crea un Intent para ir de MainActivity a RegisterActivity
        //    val intent = Intent(this, HomeActivity::class.java)

            // Inicia la nueva actividad
        //    startActivity(intent)
        //}

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Comprueba si se ha pulsado el botón "Atrás" de la ActionBar.
        // Su id es siempre 'android.R.id.home'.
        if (item.itemId == android.R.id.home) {
            // Finaliza la actividad actual y vuelve a la anterior en la pila.
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
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
        val miBoton1: Button = findViewById(R.id.buttonOption1)
        val miBoton2: Button = findViewById(R.id.buttonOption2)
        val miBoton3: Button = findViewById(R.id.buttonOption3)

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
    //TODO: mostrar de alguna forma en la Ui que el resultado a sido correcto usando estos metodos
    fun respuestaCorrecta(boton: Button ){
        val text: TextView= findViewById(R.id.ResultadoJuego)
        text.visibility = View.VISIBLE
        text.text= "Respuesta correcta"
        text.setBackgroundColor(Color.parseColor("#4CAF50"))
        text.alpha = 0f
        text.animate().alpha(1f).setDuration(300).start()

    }

    //Para cargar de otras dependencias cambiar el documentPath
    fun cargaBDImagenes(){
        val db = FirebaseFirestore.getInstance()
        db.collection("imagenes")
            .document("silabas")
            .collection("silabas")
            .get()
            .addOnSuccessListener { resultadoTextView ->
                for (document in resultadoTextView) {
                    val nombre = document.getString("nombre")
                    val url = document.getString("url")

                    Log.d("Firestore", "Image: $nombre -> $url")

                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener imágenes", e)
            }
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
