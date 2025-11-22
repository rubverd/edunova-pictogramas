package com.example.edunova

import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.load

import com.example.edunova.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
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
    private var fallos = 0

    private var indice = 0

    private var listaDeIds: List<String> = emptyList()

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLearnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos TextToSpeech
        tts = TextToSpeech(this, this)

        configurarListeners()
        iniciarJuego()

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

        binding.buttonNext.setOnClickListener {
            binding.toggleGroupOptions.clearChecked()
            avanzarSiguienteRonda()
        }

        binding.buttonJugarDeNuevo.setOnClickListener {
            reiniciarActividad()
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



    private fun cargarPictograma(pictogramaId: String) {
        // Accedemos a la colección 'pictogramas' y obtenemos el documento específico
        mostrarCargando(true)
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
            }finally {
                mostrarCargando(false)
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
            fallos++
            // Buscamos y resaltamos el botón que SÍ tenía la respuesta correcta.
            val botonCorrecto = find { it.text == palabraCorrectaActual }
            botonCorrecto?.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde

            // Feedback con mensaje
            Toast.makeText(this, "La respuesta correcta era: $palabraCorrectaActual", Toast.LENGTH_LONG).show()
        }

        binding.buttonConfirm.visibility = View.INVISIBLE
        binding.buttonNext.visibility = View.VISIBLE

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

    /**
     * Obtiene una lista de IDs de documentos de forma aleatoria desde la colección "pictogramas" en Firestore.
     *
     * @param count El número de IDs aleatorios que se desean obtener (en tu caso, 10).
     * @return Una lista de Strings con los IDs aleatorios. Si ocurre un error o no hay suficientes, puede devolver una lista más pequeña o vacía.
     */
    private suspend fun getRandomPictogramaIds(count: Int): List<String> {
        Log.d("Firestore", "Iniciando la obtención de IDs aleatorios...")

        try {
            // 1. Apuntar a la colección "pictogramas" y usar .get() para obtener todos los documentos.
            //    Usamos .await() para esperar de forma asíncrona a que la tarea de Firebase termine.
            val snapshot = db.collection("palabras").get().await()

            // 2. Extraer solo los IDs de los documentos obtenidos.
            //    El resultado de `snapshot.documents` es una lista de todos los documentos.
            //    Usamos `map { it.id }` para transformar esa lista de documentos en una lista de sus IDs (String).
            val todosLosIds = snapshot.documents.map { it.id }

            Log.d("Firestore", "Se encontraron ${todosLosIds.size} IDs en total.")

            if (todosLosIds.isEmpty()) {
                Log.w("Firestore", "La colección 'pictogramas' está vacía o no se pudo leer.")
                return emptyList()
            }

            // 3. Barajar (mezclar) la lista completa de IDs de forma aleatoria.
            val idsBarajados = todosLosIds.shuffled()

            // 4. Tomar los primeros 'count' elementos de la lista ya barajada.
            //    Usamos .take(count) que de forma segura toma como máximo 'count' elementos.
            //    Si la lista tiene menos de 'count' elementos, los tomará todos sin dar un error.
            val idsSeleccionados = idsBarajados.take(count)

            Log.d("Firestore", "Se han seleccionado ${idsSeleccionados.size} IDs aleatorios: $idsSeleccionados")

            return idsSeleccionados

        } catch (e: Exception) {
            // Si algo va mal (ej. no hay conexión a internet, problemas de permisos de Firestore),
            // se capturará la excepción aquí.
            Log.e("Firestore", "Error al obtener los IDs de pictogramas", e)
            // Devolvemos una lista vacía para que el juego pueda manejar el error de forma segura.
            return emptyList()
        }
    }

    private fun iniciarJuego(){
        aciertos = 0
        lifecycleScope.launch {
            // Llamamos a nuestra función de suspensión. El código esperará aquí
            // hasta que getRandomPictogramaIds termine y devuelva la lista.
            listaDeIds = getRandomPictogramaIds(10)

            Log.d("Firestore", "Iniciando la carga de imagen ${listaDeIds[indice]}")
            cargarPictograma(listaDeIds[indice])

        }
    }

    // Dentro de JuegoPalabrasActivity.kt

    private fun avanzarSiguienteRonda() {
        // 1. Incrementamos el índice para pasar al siguiente ID de la lista.
        indice++
        binding.buttonConfirm.isEnabled = true
        // 2. Comprobamos si hemos recorrido toda la lista.
        if (indice >= listaDeIds.size) {
            // Si el índice es igual o mayor al tamaño de la lista, ya no hay más pictogramas.
            // ¡El juego ha terminado!
            finalizarJuego()
        } else {
            // Si todavía quedan IDs en la lista, cargamos el pictograma correspondiente al nuevo índice.

                // Este código SÓLO se ejecutará DESPUÉS de que el reseteo haya finalizado.
            Log.d("Juego", "El reseteo ha completado. Ahora cargando el siguiente pictograma.")

            resetearEstadoRonda()
            val siguienteId = listaDeIds[indice]
            cargarPictograma(siguienteId)
            binding.buttonNext.visibility = View.INVISIBLE

            // IMPORTANTE: Asegúrate de que el botón "Next" esté oculto al empezar una nueva ronda.
            // Solo debe aparecer después de que el usuario responda.

        }

    }
    private fun finalizarPartida() {
        finish()
    }

    private fun resetearEstadoRonda() {
        Log.d("Juego", "--- resetearEstadoRonda: SOLICITUD DE RESETEO ---")

        // Usamos post() para garantizar que el reseteo ocurre en el momento adecuado.
        val botones = listOf(binding.buttonOption1, binding.buttonOption2, binding.buttonOption3)

        // --- PASO 1: DESTRUCCIÓN MANUAL E INDIVIDUAL ---
        // A cada botón, individualmente, le forzamos un estado base.
        Log.d("Juego", "Paso 1: Forzando isChecked=false y colores simples a cada botón.")

        for (boton in botones) {
            // Ponemos un color estático para anular cualquier selector (verde/rojo de la ronda anterior).
            // Esto es CRUCIAL para romper el estado visual "corrupto".
            boton.backgroundTintList = null // Quita cualquier tinte
            boton.strokeColor = null // Quita cualquier tinte del borde

            // FORZAMOS el estado lógico a 'false'.
            boton.isChecked = false
        }

        // --- PASO 2: LIMPIEZA DEL GRUPO ---
        // Ahora que los hijos están "tontos", le decimos al padre que olvide la selección.
        Log.d("Juego", "Paso 2: Llamando a clearChecked().")
        (binding.toggleGroupOptions as? MaterialButtonToggleGroup)?.clearChecked()

        // --- PASO 3: RECONSTRUCCIÓN DEL ESTADO ---
        // Les devolvemos su comportamiento dinámico cargando los selectores de nuevo.
        Log.d("Juego", "Paso 3: Re-aplicando los selectores de estado a los botones.")
        val fondoSelector = ContextCompat.getColorStateList(this, R.color.option_button_background_color)
        val bordeSelector = ContextCompat.getColorStateList(this, R.color.option_button_stroke_color)
        val textoSelector = ContextCompat.getColorStateList(this, R.color.black)

        for (boton in botones) {
            boton.backgroundTintList = fondoSelector
            boton.strokeColor = bordeSelector
            boton.setTextColor(textoSelector)
            boton.isEnabled = true
            boton.isChecked = false
        }

        // --- PASO 4: RESTAURAR BOTONES DE ACCIÓN ---

        binding.buttonNext.visibility = View.INVISIBLE

        // Log de verificación inmediato.
        for ((index, boton) in botones.withIndex()) {
            Log.d("Juego", "  - Estado final del Botón ${index + 1}: isChecked = ${boton.isChecked}")
        }

    }

    private fun mostrarCargando(estaCargando: Boolean) {
        if (estaCargando) {
            // Mostrar el ProgressBar y deshabilitar los botones
            binding.progressBarGame.visibility = View.VISIBLE
            binding.buttonConfirm.isEnabled = false
            binding.buttonOption1.isEnabled = false
            binding.buttonOption2.isEnabled = false
            binding.buttonOption3.isEnabled = false
        } else {
            // Ocultar el ProgressBar y habilitar los botones
            binding.progressBarGame.visibility = View.GONE
            // OJO: Solo habilitamos los botones de opción.
            // El de confirmar se gestiona en el listener del ToggleGroup.
            binding.buttonOption1.isEnabled = true
            binding.buttonOption2.isEnabled = true
            binding.buttonOption3.isEnabled = true
            binding.buttonConfirm.isEnabled = true
        }
    }

    private fun finalizarJuego() {
        // Ocultar vistas del juego usando el Group que definimos en el XML
        binding.gameContentGroup.visibility = View.GONE

        // Mostrar vistas del resumen
        binding.resumenLayout.visibility = View.VISIBLE

        // --- LÍNEAS CORREGIDAS ---
        // Usamos los recursos de string correctos que contienen el formato "%d"
        binding.textViewResumenAciertos.text = getString(R.string.texto_aciertos, aciertos)
        binding.textViewResumenFallos.text = getString(R.string.texto_fallos, fallos)
    }

    private fun reiniciarActividad() {
        // Ocultar vistas del resumen
        binding.resumenLayout.visibility = View.GONE

        // Mostrar vistas del juego
        binding.gameContentGroup.visibility = View.VISIBLE

        // 2. Reiniciar los contadores y el recorrido
        aciertos = 0
        fallos = 0
        listaDeIds = emptyList()
        indice = 0
        resetearEstadoRonda()
        iniciarJuego()
    }
}



