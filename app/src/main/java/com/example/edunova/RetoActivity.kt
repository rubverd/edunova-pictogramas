package com.example.edunova

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.with
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat

import androidx.lifecycle.lifecycleScope

import com.example.edunova.databinding.JuegoRetoBinding
import com.bumptech.glide.Glide
import com.example.edunova.databinding.SilabasBinding
import java.util.Locale
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.DocumentSnapshot
import kotlin.text.uppercase
import kotlin.text.uppercaseChar
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class RetoActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: JuegoRetoBinding
    private lateinit var letterMap: Map<Char, TextView>
    private lateinit var tts: TextToSpeech
    private var indiceGrupoActual: Int = -1
    private var aciertos = 0
    private var fallos = 0
    private var palabraActual: String? = null

    private var tiempoInicioJuego: Long = 0L

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val palabrasUsadasEnElRosco = mutableListOf<String>()
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
            reproducirSonido(palabraActual.toString())
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
        tiempoInicioJuego = System.currentTimeMillis()
        avanzarAlSiguienteGrupo()
    }

    private fun avanzarAlSiguienteGrupo() {
        indiceGrupoActual++

        // La comprobación del final del juego ya no es necesaria aquí.
        // Solo avanzamos si no hemos llegado al final.
        if (indiceGrupoActual < 27) {
            val letraActual = abecedarioEspanol[indiceGrupoActual]
            // 1. Asignamos esa letra como la respuesta correcta que esperamos.

            lifecycleScope.launch {
                //1. Intenta la búsqueda primaria (empieza por...)
                var documentoPalabra: DocumentSnapshot? = obtenerPalabraPorLetra(letraActual)

                // 2. Si falla, llama a la función de búsqueda secundaria (la que acabamos de crear)
                if (documentoPalabra == null) {
                    documentoPalabra = obtenerPalabraQueContengaLetra(letraActual, palabrasUsadasEnElRosco)
                }

                // 3. El resto del código que procesa el documento es idéntico.
                if (documentoPalabra != null) {
                    // ¡Palabra encontrada! Extraemos los datos.
                    val palabra = documentoPalabra.getString("palabra") ?: "Error"
                    val definicion = documentoPalabra.getString("definicion") ?: "Sin definición"

                    // --- ¡NUEVO CÓDIGO AQUÍ! ---
                    // 1. Obtenemos la URL de la imagen del mismo documento.
                    val urlImagen = documentoPalabra.getString("urlImagen")

                    // 2. Usamos Glide para cargar la imagen.
                    if (!urlImagen.isNullOrEmpty()) {
                        // Si la URL no es nula ni vacía...
                        binding.imagenReto.visibility = View.VISIBLE // Hacemos visible el ImageView
                        Glide.with(this@RetoActivity) // Contexto (la actividad)
                            .load(urlImagen)             // La URL de la imagen a cargar // (Opcional) Imagen si hay un error
                            .into(binding.imagenReto) // El ImageView donde se mostrará
                    } else {
                        // Si no hay URL, ocultamos el ImageView para que no ocupe espacio.
                        binding.imagenReto.visibility = View.GONE
                    }


                    // Guardas la palabra correcta para poder comprobarla después
                    palabraActual = palabra
                    palabrasUsadasEnElRosco.add(palabra)
                    mostrarPista(letraActual,palabraActual.toString())
                    // Habilitas los controles para el usuario
                    binding.respuesta.isEnabled = true
                    binding.botonConfirmar.isEnabled = true
                } else {
                    // No se encontró palabra, ocultamos el ImageView por si acaso
                    binding.imagenReto.visibility = View.GONE
                    Log.w("Rosco", "No hay palabra para la letra '$letraActual', saltando turno.")
                    // ... resto de la lógica de error ...
                }
            }


            // 2. Mostramos en la UI qué letra se está trabajando.
            // Puedes mostrar la letra directamente o un mensaje como "Letra C".
            //binding.TextoSilabas.text = "Adivina la letra: ${letraActual.uppercase()}" // Ejemplo

            // 3. Limpiamos el campo de texto para la nueva respuesta.
            binding.respuesta.text?.clear()

        } else {
            // El juego ha terminado.
            Toast.makeText(this, "¡Juego completado!", Toast.LENGTH_LONG).show()
            binding.botonConfirmar.isEnabled = false // Deshabilitar el botón de confirmar.
            finalizarReto()
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
        val nombrePictograma = palabraActual

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

    /**
     * Busca en Firestore una palabra aleatoria que empiece por una letra específica.
     *
     * @param letra La letra por la que debe empezar la palabra (ej. "A", "B", "C").
     * @return Un objeto DocumentSnapshot que contiene la palabra y su definición, o null si no se encuentra ninguna.
     */
    private suspend fun obtenerPalabraPorLetra(letra: Char): DocumentSnapshot? {
        val letraMayuscula = letra.uppercaseChar()
        Log.d("Rosco", "Buscando en Firestore una palabra que empiece por: '$letraMayuscula'")

        try {
            // --- LA LÓGICA DE LA CONSULTA ---
            // Firestore no tiene un operador "empieza por" (startsWith).
            // El truco es usar un rango:
            // 1. whereGreaterThanOrEqualTo("palabra", letraMayuscula.toString()) -> palabras >= "A"
            // 2. whereLessThan("palabra", letraSiguiente) -> palabras < "B"
            // Esto nos da todas las palabras que están entre "A" (inclusive) y "B" (exclusive),
            // es decir, todas las que empiezan por "A".

            val letraSiguiente = (letraMayuscula.code + 1).toChar().toString()

            val querySnapshot = db.collection("palabras")
                .whereGreaterThanOrEqualTo("palabra", letraMayuscula.toString())
                .whereLessThan("palabra", letraSiguiente)
                .get()
                .await() // .await() es de la librería kotlinx-coroutines-play-services

            if (querySnapshot.isEmpty) {
                Log.w("Rosco", "No se encontraron palabras para la letra '$letraMayuscula'")
                return null
            }

            // De todos los resultados, elegimos uno al azar.
            val palabraEncontrada = querySnapshot.documents.random()
            Log.d("Rosco", "Palabra encontrada para '$letraMayuscula': ${palabraEncontrada.getString("palabra")}")
            return palabraEncontrada

        } catch (e: Exception) {
            Log.e("Rosco", "Error al obtener palabra de Firestore para la letra '$letraMayuscula'", e)
            // Puedes mostrar un Toast o manejar el error de red aquí
            return null
        }
    }

    private fun obtenerSilabasPosibles(letra: Char): List<String> {val letraMayus = letra.uppercase()
        val vocales = listOf("A", "E", "I", "O", "U")

        // Genera sílabas simples de dos letras (ej: "PA", "PE", "PI", "PO", "PU")
        return vocales.map { vocal -> "$letraMayus$vocal" }
    }

    /**
     * Busca en Firestore una palabra que CONTENGA una letra, usando el campo 'silabas'.
     * * @param letra La letra que debe contener la palabra.
     * @param palabrasExcluidas Lista de palabras ya usadas para no repetir.
     * @return Un DocumentSnapshot de la palabra encontrada, o null.
     */
    private suspend fun obtenerPalabraQueContengaLetra(letra: Char, palabrasExcluidas: List<String>): DocumentSnapshot? {
        val letraMayuscula = letra.uppercaseChar()
        Log.d("Rosco", "BÚSQUEDA SECUNDARIA (con sílabas): Buscando palabra que contenga '$letraMayuscula'")

        try {
            // --- FASE 1: BÚSQUEDA APROXIMADA EN FIRESTORE ---
            // Generamos una lista de sílabas comunes para la consulta.
            // Esto es opcional pero ayuda a reducir los documentos traídos.
            // Si quieres traer TODAS las palabras y filtrar en la app, puedes saltarte este paso
            // y hacer el 'db.collection("palabras").get().await()' directamente,
            // pero sería menos eficiente.
            // Por ahora, vamos a mantener el enfoque más optimizado.

            // Por simplicidad, vamos a traer todas las palabras y filtrar en el cliente.
            // Para bases de datos muy grandes, se necesitaría una estrategia de indexación más compleja.
            val querySnapshot = db.collection("palabras").get().await()

            if (querySnapshot.isEmpty) {
                Log.w("Rosco", "La colección 'palabras' está vacía.")
                return null
            }

            // --- FASE 2: FILTRADO PRECISO EN KOTLIN ---
            val resultadosValidos = querySnapshot.documents.filter { doc ->
                // Condición 1: La palabra no debe haber sido usada ya.
                val palabraActual = doc.getString("palabra") ?: ""
                val noExcluida = !palabrasExcluidas.contains(palabraActual)

                if (!noExcluida) return@filter false // Si ya fue usada, la descartamos.

                // Condición 2: Alguna de sus sílabas debe contener la letra.
                val silabas = doc.get("silabas") as? List<String>
                val contieneLaLetra = silabas?.any { silaba ->
                    silaba.uppercase().contains(letraMayuscula)
                } ?: false

                // El documento es válido si cumple ambas condiciones.
                contieneLaLetra
            }

            if (resultadosValidos.isEmpty()) {
                Log.w("Rosco", "Filtrado final no encontró palabras que contengan '$letraMayuscula' en sus sílabas.")
                return null
            }

            // De los resultados válidos y filtrados, elegimos uno al azar.
            Log.d("Rosco", "Se encontraron ${resultadosValidos.size} candidatos válidos en la búsqueda secundaria.")
            return resultadosValidos.random()

        } catch (e: Exception) {
            Log.e("Rosco", "Error en la búsqueda secundaria (con sílabas) para '$letraMayuscula'", e)
            return null
        }
    }
    private fun finalizarReto(){
        binding.gameContentGroup.visibility = View.GONE
        // Mostrar vistas del resumen
        binding.resumenLayout.visibility = View.VISIBLE

        val tiempoFinalJuego = System.currentTimeMillis()
        val tiempoTotalMs = tiempoFinalJuego - tiempoInicioJuego // Tiempo en milisegundos
        val segundosTotales = tiempoTotalMs / 1000

        // PASO 3.2: Formatea el tiempo a minutos y segundos
        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60
        val tiempoFormateado = String.format(Locale.getDefault(),"%02d:%02d", minutos, segundos)


        binding.textViewResumenAciertos.text = getString(R.string.texto_aciertos, aciertos)
        binding.textViewResumenFallos.text = getString(R.string.texto_fallos, fallos)
        binding.textViewResumenTiempo.text = getString(R.string.texto_tiempo, tiempoFormateado)
    }

    private fun mostrarPista(letra: Char, palabra: String){


        val pistaArray = CharArray(palabra.length) { '_' }

        // 2. Buscamos la primera aparición de la letra del turno en la palabra (ignorando mayúsculas/minúsculas).
        // 'indexOf' devuelve -1 si no la encuentra.
        val posicionLetra = palabra.indexOf(letra, ignoreCase = true)

        // 3. Si la letra se encuentra en la palabra, la revelamos en nuestra pista.
        if (posicionLetra != -1) {
            pistaArray[posicionLetra] = palabra[posicionLetra]
        }

        val pistaConEspacios = pistaArray.joinToString(separator = " ")

        binding.textoPista.visibility = View.VISIBLE
        binding.textoPista.text = pistaConEspacios

    }





}