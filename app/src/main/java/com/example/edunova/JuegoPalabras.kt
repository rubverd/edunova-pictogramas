package com.example.edunova

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable
import com.example.edunova.databinding.ActivityLearnBinding
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load

import com.example.edunova.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.io.path.exists


class JuegoPalabras : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var binding: ActivityLearnBinding

    private var glideExecuted = false

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLearnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos TextToSpeech
        tts = TextToSpeech(this, this)


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
    fun cargaBDImagenes(pictogramaId: String){
        val db = FirebaseFirestore.getInstance()
        db.collection("imagenes")
            .document("silabas")
            .collection("silabas")
            .get()
            .addOnSuccessListener { resultadoTextView ->
                for (document in resultadoTextView) {
                    val nombre = document.getString(pictogramaId)
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

    private fun cargarPictograma(pictogramaId: String) {
        // Accedemos a la colección 'pictogramas' y obtenemos el documento específico
        db.collection("palabras").document(pictogramaId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // El documento fue encontrado
                    Log.d("Firebase", "Datos del documento: ${document.data}")

                    // 3. Obtenemos la URL del campo "imageUrl"
                    // Usamos .getString() para evitar errores si el campo no es un String.
                    val imageUrl = document.getString("urlImagen")

                    if (!imageUrl.isNullOrEmpty()) {
                        // ¡Aquí ocurre la magia!
                        // 4. Usamos Glide para cargar la URL en nuestro ImageView
                        binding.imageViewPictogram.load(imageUrl)

                    } else {
                        // El campo imageUrl está vacío o no existe
                        Toast.makeText(this, "La URL de la imagen no fue encontrada.", Toast.LENGTH_SHORT).show()
                        Log.w("Firebase", "El campo 'imageUrl' está vacío o es nulo.")
                    }

                } else {
                    // El documento no existe
                    Log.w("Firebase", "No se encontró un documento con el ID: $pictogramaId")
                    Toast.makeText(this, "No se encontró el pictograma.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                // Ocurrió un error al intentar obtener el documento
                Log.e("Firebase", "Error al obtener el documento", exception)
                Toast.makeText(this, "Error al cargar los datos.", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Si la actividad tiene el foco Y nuestro código de Glide no se ha ejecutado todavía
        if (hasFocus && !glideExecuted) {

            // Marcamos la bandera para evitar que se ejecute de nuevo
            // si la actividad pierde y recupera el foco (ej: al bajar la cortina de notificaciones)
            glideExecuted = true


            Log.d("DEBUG_CHECK", "onWindowFocusChanged con foco. Ejecutando Glide.")

            Log.d("DEBUG_CHECK", "Binding object: $binding")
            Log.d("DEBUG_CHECK", "ImageView object: ${binding.imageViewPictogram}")
            val imageUrl =
                "https://ik.imagekit.io/pkzchzcdv/pictogramaPruebaCasa.jpg" // <-- Usando la de Imgur que sabemos que es directa

            Glide.with(binding.imageViewPictogram.context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {

                    // FIRMA CORREGIDA PARA onLoadFailed
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>, // Cambiado de Target<Drawable!> a Target<Drawable> que es más idiomático en Kotlin
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("GLIDE_ERROR", "Fallo al cargar la imagen. URL: $model", e)
                        return false // Deja que Glide muestre la imagen de 'error'
                    }

                    // FIRMA CORREGIDA PARA onResourceReady
                    override fun onResourceReady(
                        resource: Drawable, // Cambiado de Drawable & Any a simplemente Drawable
                        model: Any,
                        target: Target<Drawable>?, // Target puede ser nulo aquí
                        dataSource: DataSource,

                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("GLIDE_SUCCESS", "Imagen cargada correctamente desde: $dataSource")
                        return false // Deja que Glide muestre la imagen cargada
                    }
                })
                .into(binding.imageViewPictogram)

            binding.imageViewPictogram.postDelayed({
                val drawable = binding.imageViewPictogram.drawable
                Log.d("IV_CONTENT_CHECK", "El drawable del ImageView es: $drawable")

                if (drawable != null) {
                    // Si el drawable no es nulo, podemos obtener más información.
                    // intrinsicWidth y intrinsicHeight son las dimensiones originales de la imagen.
                    val width = drawable.intrinsicWidth
                    val height = drawable.intrinsicHeight
                    Log.d("IV_CONTENT_CHECK", "Dimensiones del drawable: ${width}x${height}")
                }
            }, 500) // Esperamos 500 milisegundos

            //val pictogramaIdParaCargar = "id_Manzana"
            //cargarPictograma(pictogramaIdParaCargar)

            val botonHablar = findViewById<FloatingActionButton>(R.id.fabPlaySound)
            botonHablar.setOnClickListener {
                reproducirTexto("Hola, este es un ejemplo de texto a voz en Kotlin.")
            }

            val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
            botonVolver.setOnClickListener {
                // Crea un Intent para ir de MainActivity a RegisterActivity
                val intent = Intent(this, HomeActivity::class.java)

                // Inicia la nueva actividad
                startActivity(intent)
            }

        }
    }
}


