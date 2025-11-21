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
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.lifecycle.lifecycleScope
import coil.load

import com.example.edunova.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldPath
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import kotlin.io.path.exists


class JuegoPalabras : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var binding: ActivityLearnBinding

    private var palabraCorrectaActual: String? = null
    private var aciertos = 0

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLearnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos TextToSpeech
        tts = TextToSpeech(this, this)

        configurarListeners()
        cargarPictograma("id_Manzana")

    }

    private fun configurarListeners() {
        // Listener para el grupo de opciones
        binding.toggleGroupOptions.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                // Si se selecciona un botón, se muestra el de confirmar
                binding.buttonConfirm.visibility = View.VISIBLE
            }
        }

        // Listener para el botón de confirmar
        binding.buttonConfirm.setOnClickListener {
            Log.d("DEBUG_JUEGO", "¡CLICK! El botón Confirmar ha sido pulsado.")
            comprobarRespuesta() // La función que evalúa si es correcto o no
        }

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener {
            // Crea un Intent para ir de MainActivity a RegisterActivity
            finish()
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
        lifecycleScope.launch {
            try {
                // --- PARTE 1: OBTENER LA INFORMACIÓN DEL PICTOGRAMA CORRECTO ---
                val documentSnapshot = db.collection("palabras").document(pictogramaId).get().await()

                if (documentSnapshot == null || !documentSnapshot.exists()) {
                    Log.e("Juego", "No se encontró el documento del pictograma con ID: $pictogramaId")
                    Toast.makeText(this@JuegoPalabras, "Error al cargar pictograma.", Toast.LENGTH_SHORT).show()
                    return@launch // Salimos de la corrutina si no hay documento
                }

                val imageUrl = documentSnapshot.getString("urlImagen")
                val palabraCorrecta = documentSnapshot.getString("palabra")

                if (imageUrl.isNullOrEmpty() || palabraCorrecta.isNullOrEmpty()) {
                    Log.e("Juego", "El documento $pictogramaId no tiene urlImagen o palabra.")
                    Toast.makeText(this@JuegoPalabras, "Datos del pictograma incompletos.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                palabraCorrectaActual = palabraCorrecta

                Log.d("DEBUG_JUEGO", "Palabra correcta guardada en 'palabraCorrectaActual': ${palabraCorrectaActual}")

                // Cargar la imagen
                binding.imageViewPictogram.load(imageUrl)

                // --- PARTE 2: ASIGNAR LA PALABRA CORRECTA A UN BOTÓN ALEATORIO ---
                val botones = mutableListOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)
                val botonCorrecto = botones.random()
                botonCorrecto.text = palabraCorrecta

                val botonHablar = findViewById<FloatingActionButton>(R.id.fabPlaySound)
                botonHablar.setOnClickListener {
                    reproducirTexto(palabraCorrecta)
                }

                // Quitamos el botón ya usado de la lista. Ahora 'botones' solo contiene los 2 botones restantes.
                botones.remove(botonCorrecto)

                // --- PARTE 3: OBTENER PALABRAS INCORRECTAS Y ASIGNARLAS ---
                val palabrasIncorrectas = getPalabrasAleatoriasIncorrectas(palabraCorrecta, 2)

                if (palabrasIncorrectas.size < 2) {
                    Log.e("Juego", "No se pudieron obtener suficientes palabras incorrectas.")
                    // Aquí podrías tener una lista de palabras de respaldo por si falla Firebase
                    botones.forEach { it.text = "Error" }
                    return@launch
                }

                // Asignamos las palabras incorrectas a los dos botones restantes
                botones[0].text = palabrasIncorrectas[0]
                botones[1].text = palabrasIncorrectas[1]

            } catch (e: Exception) {
                Log.e("Juego", "Ocurrió una excepción al cargar el pictograma", e)
                Toast.makeText(this@JuegoPalabras, "Error de conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
    * Obtiene una lista de palabras aleatorias de la colección "palabras",
    * asegurándose de que no incluye la palabra correcta.
    *
    * @param palabraAExcluir La palabra correcta que no debe ser incluida en los resultados.
    * @param cantidad La cantidad de palabras aleatorias que se necesitan.
    * @return Una lista de Strings con las palabras incorrectas.
    */
    private suspend fun getPalabrasAleatoriasIncorrectas(palabraAExcluir: String, cantidad: Int): List<String> {
        val palabrasCollection = db.collection("palabras")
        val randomId = db.collection("some_collection").document().id // Punto de partida aleatorio

        try {
            // Hacemos una consulta pidiendo documentos que no sean la palabra correcta,
            // empezando desde un punto aleatorio.
            // Nota: Firestore no permite combinar `whereNotEqualTo` con `whereGreaterThanOrEqualTo` en campos diferentes.
            // La estrategia es pedir más y filtrar después.
            val snapshot = palabrasCollection
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), randomId)
                .limit((cantidad + 5).toLong()) // Pedimos más para tener margen al filtrar
                .get()
                .await()

            // Mapeamos los resultados a una lista de Strings (palabras) y filtramos la que no queremos
            val palabrasObtenidas = snapshot.documents
                .mapNotNull { it.getString("palabra") }
                .filter { it != palabraAExcluir }
                .distinct() // Nos aseguramos de que no haya duplicados

            if (palabrasObtenidas.size >= cantidad) {
                return palabrasObtenidas.take(cantidad) // Devolvemos la cantidad exacta que necesitamos
            }

            // Si no obtuvimos suficientes, hacemos una segunda consulta desde el principio
            val snapshot2 = palabrasCollection
                .limit((cantidad + 5).toLong())
                .get()
                .await()

            val palabrasObtenidas2 = snapshot2.documents
                .mapNotNull { it.getString("palabra") }
                .filter { it != palabraAExcluir }
                .distinct()

            // Combinamos ambas listas, nos aseguramos de no tener duplicados, y tomamos la cantidad necesaria
            return (palabrasObtenidas + palabrasObtenidas2).distinct().take(cantidad)

        } catch (e: Exception) {
            Log.e("Juego", "Error al obtener palabras aleatorias incorrectas", e)
            return emptyList()
        }
    }

    private fun comprobarRespuesta() {
        // --- 1. OBTENER LA RESPUESTA DEL USUARIO ---
        // Obtenemos el ID del botón que está seleccionado dentro del ToggleGroup.
        val idBotonSeleccionado = binding.toggleGroupOptions.checkedButtonId

        // Salvaguarda: Si por alguna razón no hay ningún botón seleccionado o no tenemos
        // la palabra correcta guardada, no hacemos nada y salimos de la función.
        if (idBotonSeleccionado == View.NO_ID || palabraCorrectaActual == null) {
            Log.e("Juego", "ComprobarRespuesta fue llamado sin una opción seleccionada o sin palabra correcta.")
            return
        }

        // Obtenemos la referencia al botón concreto usando su ID.
        val botonSeleccionado = findViewById<Button>(idBotonSeleccionado)
        val respuestaUsuario = botonSeleccionado.text.toString()

        // --- 2. DESHABILITAR CONTROLES ---
        // Congelamos la pantalla para que el usuario no pueda cambiar su respuesta
        // o pulsar confirmar de nuevo mientras se muestra el resultado.
        binding.buttonConfirm.isEnabled = false
        // Deshabilitar todos los botones del grupo para que no se puedan pulsar.
        // Una forma sencilla es deshabilitar el grupo entero.
        (0 until binding.toggleGroupOptions.childCount).forEach {
            val button = binding.toggleGroupOptions.getChildAt(it)
            button.isEnabled = false
        }

        // --- 3. COMPARAR Y DAR FEEDBACK ---
        val esRespuestaCorrecta = (respuestaUsuario == palabraCorrectaActual)

        if (esRespuestaCorrecta) {
            // --- RESPUESTA CORRECTA ---
            Log.d("Juego", "Respuesta CORRECTA: $respuestaUsuario")
            aciertos++ // Incrementamos el contador de aciertos

            // Feedback visual: Ponemos el botón seleccionado en verde.
            botonSeleccionado.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde

            // Feedback con mensaje
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()

        } else {
            // --- RESPUESTA INCORRECTA ---
            Log.w("Juego", "Respuesta INCORRECTA. Usuario: '$respuestaUsuario', Correcta: '$palabraCorrectaActual'")

            // Feedback visual: Ponemos el botón seleccionado en rojo.
            botonSeleccionado.setBackgroundColor(Color.parseColor("#F44336")) // Rojo

            // Buscamos y resaltamos el botón que SÍ tenía la respuesta correcta.
            val botonCorrecto = find { it.text == palabraCorrectaActual }
            botonCorrecto?.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde

            // Feedback con mensaje
            Toast.makeText(this, "La respuesta correcta era: $palabraCorrectaActual", Toast.LENGTH_LONG).show()
        }

    }

    private fun find(predicate: (Button) -> Boolean): Button? {
        for (i in 0 until binding.toggleGroupOptions.childCount) {
            val button = binding.toggleGroupOptions.getChildAt(i) as Button
            if (predicate(button)) {
                return button
            }
        }
        return null
    }

    /*
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
                finish()
            }

        }
    }
     */
}


