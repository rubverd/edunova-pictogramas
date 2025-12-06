package com.example.edunova

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.edunova.databinding.JuegoRetoBinding
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.core.view.setMargins
import com.google.android.material.card.MaterialCardView
import android.view.ViewTreeObserver

import kotlin.math.floor
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.colorspace.connect
import androidx.compose.ui.unit.constrainHeight
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

import com.google.android.flexbox.FlexboxLayout

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.transition.TransitionManager


class RetoActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: JuegoRetoBinding
    private lateinit var letterMap: Map<Char, TextView>
    private lateinit var tts: TextToSpeech
    private lateinit var etHiddenInput: EditText
    private lateinit var layoutLetterBoxes: FlexboxLayout
    private val letterBoxViews = mutableListOf<TextView>() // Lista para guardar las vistas de las letras
    private var indiceGrupoActual: Int = -1
    private var aciertos = 0
    private var fallos = 0
    private var palabraActual: String? = null

    // --- VARIABLES DE SEGUIMIENTO ---
    private var tiempoInicioJuego: Long = 0L
    private val repository = FirebaseConnection()
    private var datosAlumno: Map<String, Any>? = null

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val palabrasUsadasEnElRosco = mutableListOf<String>()

    private val listaAciertos = mutableListOf<String>()

    private val listaFallos = mutableListOf<String>()
    private var abecedarioEspanol: MutableList<Char> = mutableListOf()

    // --- VARIABLES PARA SONIDOS ---
    private lateinit var soundPool: SoundPool
    private var sonidoAciertoId: Int = 0
    private var sonidoFalloId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = JuegoRetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        etHiddenInput = findViewById(R.id.etHiddenInput)
        layoutLetterBoxes = findViewById(R.id.layoutLetterBoxes)

        tts = TextToSpeech(this, this)

        // 1. CARGAR DATOS DEL ALUMNO
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getUserData(currentUser.uid) { data ->
                datosAlumno = data
                Log.d("RetoActivity", "Datos alumno cargados: ${datosAlumno?.get("displayName")}")
            }
        }

        initializeLetterMap()
        inicializarAbecedario()
        inicializarSoundPool() // Iniciamos sonidos
        setupInputListener() // Esta se queda igual
        setupInteraction()



        setupLetterBoxes(palabraActual)

        // 2. Configurar el listener para el EditText invisible
        setupInputListener()


        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener { finish() }



        binding.fabPlaySoundSilabas.setOnClickListener {
            reproducirSonido(palabraActual.toString())
        }

        // --- BOTONES DEL RESUMEN ---
        binding.buttonJugarDeNuevo.setOnClickListener {
            reiniciarActividad()
        }

        binding.buttonSalir.setOnClickListener {
            finish()
        }

    }

    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()



    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "El idioma español no está soportado.", Toast.LENGTH_SHORT).show()
            }
            iniciarRecorrido()
        } else {
            Toast.makeText(this, "Error al inicializar el motor de voz.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reproducirSonido(texto: String) {
        if (::tts.isInitialized && texto.isNotBlank()) {
            if (tts.voice.locale.toLanguageTag().startsWith("es")) {
                tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun iniciarRecorrido() {
        indiceGrupoActual = -1
        binding.botonConfirmar.isEnabled = true
        // 2. MARCAR TIEMPO INICIO
        tiempoInicioJuego = System.currentTimeMillis()
        avanzarAlSiguienteGrupo()
    }

    private fun avanzarAlSiguienteGrupo() {
        indiceGrupoActual++

        if (indiceGrupoActual < 27) {
            val letraActual = abecedarioEspanol[indiceGrupoActual]

            lifecycleScope.launch {
                var documentoPalabra: DocumentSnapshot? = obtenerPalabraPorLetra(letraActual, palabrasUsadasEnElRosco)

                if (documentoPalabra == null) {
                    documentoPalabra =
                        obtenerPalabraQueContengaLetra(letraActual, palabrasUsadasEnElRosco)
                }

                if (documentoPalabra != null) {
                    val palabra = documentoPalabra.getString("palabra") ?: "Error"
                    val urlImagen = documentoPalabra.getString("urlImagen")

                    if (!urlImagen.isNullOrEmpty()) {
                        binding.cardImagenReto.visibility = View.VISIBLE
                        Glide.with(this@RetoActivity)
                            .load(urlImagen)
                            .into(binding.imagenReto)
                    } else {
                        binding.cardImagenReto.visibility = View.GONE
                    }

                    palabraActual = palabra
                    palabrasUsadasEnElRosco.add(palabra)
                    setupDynamicLetterBoxes(palabraActual)

                    // 2. Configurar el listener para el EditText invisible

                    binding.botonConfirmar.isEnabled = true
                } else {
                    binding.imagenReto.visibility = View.GONE
                    Log.w("Rosco", "No hay palabra para la letra '$letraActual', saltando turno.")
                }
            }
        } else {
            Toast.makeText(this, "¡Juego completado!", Toast.LENGTH_LONG).show()
            binding.botonConfirmar.isEnabled = false
            finalizarReto()
        }
    }

    private fun inicializarAbecedario() {
        val abecedarioSinN = ('a'..'z').toMutableList()
        val indiceDeLaN = abecedarioSinN.indexOf('n')
        if (indiceDeLaN != -1) {
            abecedarioSinN.add(indiceDeLaN + 1, 'ñ')
        }
        abecedarioEspanol = abecedarioSinN
    }

    private fun verificarRespuesta(respuesta: String) {
        val nombrePictograma = palabraActual
        if (nombrePictograma == null) return

        val letraActual = abecedarioEspanol.getOrNull(indiceGrupoActual)
        val textViewDeLetra = if (letraActual != null) letterMap[letraActual.uppercaseChar()] else null

        if (respuesta.equals(nombrePictograma, ignoreCase = true)) {
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
            aciertos++
            palabraActual?.let { palabra ->
                listaAciertos.add(palabra)
            }
            soundPool.play(sonidoAciertoId, 1.0f, 1.0f, 1, 0, 1.0f)
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_correcto)
        } else {
            Toast.makeText(this, "Incorrecto. La respuesta era '$nombrePictograma'", Toast.LENGTH_SHORT).show()
            fallos++
            palabraActual?.let { palabra ->
                listaFallos.add(palabra)
            }
            soundPool.play(sonidoFalloId, 1.0f, 1.0f, 1, 0, 1.0f)
            textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.design_default_color_error)
        }
        etHiddenInput.setText("")
        avanzarAlSiguienteGrupo()
    }

    private fun initializeLetterMap() {
        letterMap = mapOf(
            'A' to binding.A, 'E' to binding.E, 'I' to binding.I, 'O' to binding.O, 'U' to binding.U,
            'B' to binding.B, 'C' to binding.C, 'D' to binding.D, 'F' to binding.F, 'G' to binding.G,
            'H' to binding.H, 'J' to binding.J, 'K' to binding.K, 'L' to binding.L, 'M' to binding.M,
            'N' to binding.N, 'Ñ' to binding.NEne, 'P' to binding.P, 'Q' to binding.Q, 'R' to binding.letraR,
            'S' to binding.S, 'T' to binding.T, 'V' to binding.V, 'W' to binding.W, 'X' to binding.X,
            'Y' to binding.Y, 'Z' to binding.Z
        )
    }

    private suspend fun obtenerPalabraPorLetra(letra: Char, palabrasExcluidas: List<String>): DocumentSnapshot? {
        val letraMayuscula = letra.uppercaseChar()
        try {
            val letraSiguiente = (letraMayuscula.code + 1).toChar().toString()
            val querySnapshot = db.collection("palabras")
                .whereGreaterThanOrEqualTo("palabra", letraMayuscula.toString())
                .whereLessThan("palabra", letraSiguiente)
                .get().await()

            if (querySnapshot.isEmpty) return null

            // --- INICIO DE LA MODIFICACIÓN ---
            // 1. Filtramos los documentos para excluir las palabras ya usadas.
            val documentosDisponibles = querySnapshot.documents.filter { documento ->
                !palabrasExcluidas.contains(documento.getString("palabra"))
            }

            // 2. Si después de filtrar no queda ninguna palabra, retornamos null.
            if (documentosDisponibles.isEmpty()) return null

            // 3. Seleccionamos una palabra al azar de la lista filtrada.
            return documentosDisponibles.random()
            // --- FIN DE LA MODIFICACIÓN ---

        } catch (e: Exception) {
            // Log del error para facilitar la depuración
            Log.e("Firestore", "Error al obtener palabra por letra '$letra'", e)
            return null
        }
    }

    private suspend fun obtenerPalabraQueContengaLetra(letra: Char, palabrasExcluidas: List<String>): DocumentSnapshot? {
        val letraMayuscula = letra.uppercaseChar()
        try {
            val querySnapshot = db.collection("palabras").get().await()
            if (querySnapshot.isEmpty) return null

            val resultadosValidos = querySnapshot.documents.filter { doc ->
                val palabraActual = doc.getString("palabra") ?: ""
                val noExcluida = !palabrasExcluidas.contains(palabraActual)
                if (!noExcluida) return@filter false

                val silabas = doc.get("silabas") as? List<String>
                silabas?.any { it.uppercase().contains(letraMayuscula) } ?: false
            }

            if (resultadosValidos.isEmpty()) return null
            return resultadosValidos.random()
        } catch (e: Exception) {
            return null
        }
    }

    private fun finalizarReto() {
        //binding.gameContentGroup.visibility = View.GONE
        binding.resumenLayout.visibility = View.VISIBLE

        val tiempoFinalJuego = System.currentTimeMillis()
        val tiempoTotalMs = tiempoFinalJuego - tiempoInicioJuego
        val segundosTotales = tiempoTotalMs / 1000

        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60
        val tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

        binding.textViewResumenAciertos.text = getString(R.string.texto_aciertos, aciertos)
        binding.textViewResumenFallos.text = getString(R.string.texto_fallos, fallos)
        binding.textViewResumenTiempo.text = getString(R.string.texto_tiempo, tiempoFormateado)

        // 3. GUARDAR RESULTADOS
        guardarResultadosEnBD(segundosTotales)
    }

    private fun guardarResultadosEnBD(segundosTotales: Long) {
        val currentUser = repository.getCurrentUser() ?: return

        val intentoData = hashMapOf(
            "studentUid" to currentUser.uid,
            "studentName" to (datosAlumno?.get("displayName") ?: "Alumno"),
            "school" to (datosAlumno?.get("school") ?: "Sin Centro"),
            "classroom" to (datosAlumno?.get("classroom") ?: "Sin Clase"),
            "exerciseType" to "reto_abecedario",
            "timestamp" to System.currentTimeMillis(),
            "timeSpentSeconds" to segundosTotales,
            "score" to aciertos,
            "totalQuestions" to 27,
            "status" to "completed",
            "aciertos" to listaAciertos,
            "fallos" to listaFallos
        )

        repository.saveStudentAttempt(intentoData) { success ->
            if (success) Log.d("RetoActivity", "Progreso guardado correctamente")
        }
    }

    private fun mostrarPista(letra: Char, palabra: String) {
        val pistaArray = CharArray(palabra.length) { '_' }
        val posicionLetra = palabra.indexOf(letra, ignoreCase = true)
        if (posicionLetra != -1) {
            pistaArray[posicionLetra] = palabra[posicionLetra]
        }
        val pistaConEspacios = pistaArray.joinToString(separator = " ")
    }

    private fun reiniciarActividad() {
        // Resetear vista
        binding.resumenLayout.visibility = View.GONE
       // binding.gameContentGroup.visibility = View.VISIBLE

        // Resetear colores de letras
        letterMap.values.forEach { textView ->
            textView.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gris_contraste) // Asegúrate de tener este color o usa otro gris
        }

        // Resetear lógica
        aciertos = 0
        fallos = 0
        palabrasUsadasEnElRosco.clear()
        palabraActual = null

        iniciarRecorrido()
    }

    private fun inicializarSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        sonidoAciertoId = soundPool.load(this, R.raw.sonido_correcto, 1)
        sonidoFalloId = soundPool.load(this, R.raw.sonido_incorrecto, 1)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }

    private fun setupLetterBoxes(palabra: String?) {
        layoutLetterBoxes.removeAllViews() // Limpiar vistas anteriores
        letterBoxViews.clear()
        if (palabra != null) {
            for (char in palabra) {
                // Inflar el layout de la caja de letra
                val inflater = LayoutInflater.from(this)
                val letterBoxView =
                    inflater.inflate(R.layout.item_letter_box, layoutLetterBoxes, false)

                // Encontrar el TextView dentro de la caja
                val tvLetter = letterBoxView.findViewById<TextView>(R.id.tvLetter)

                // Añadir la vista a nuestro LinearLayout y a la lista de control
                layoutLetterBoxes.addView(letterBoxView)
                letterBoxViews.add(tvLetter)
            }
        }
    }

    private fun setupInputListener() {
        etHiddenInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val textoIngresado = s.toString().uppercase()

                // Actualizar las cajas de letras con el texto ingresado
                for (i in letterBoxViews.indices) {
                    if (i < textoIngresado.length) {
                        letterBoxViews[i].text = textoIngresado[i].toString()
                    } else {
                        letterBoxViews[i].text = "" // Limpiar las cajas restantes
                    }
                }

                binding.botonConfirmar.setOnClickListener {
                    if (palabraActual != null) {
                        verificarRespuesta(textoIngresado)
                    }
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }
        })
    }

    private fun showKeyboardAndFocus() {
        // 1. Aseguramos que nuestro EditText es teóricamente enfocable
        etHiddenInput.isFocusable = true
        etHiddenInput.isFocusableInTouchMode = true
        Log.d("ModoReto_Setup", "etHiddenInput configurado como enfocable.")

        // 2. Nos suscribimos al evento del árbol de vistas
        //    Esto se disparará cuando el layout esté 100% dibujado y listo.
        val viewTreeObserver = etHiddenInput.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // --- ESTE CÓDIGO SE EJECUTA EN EL MOMENTO PERFECTO ---

                // 3. Una vez se ejecuta, nos damos de baja para no volver a llamarlo
                etHiddenInput.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // 4. Intentamos obtener el foco
                etHiddenInput.requestFocus()
                Log.d("ModoReto_GLL", "GlobalLayoutListener: requestFocus() llamado.")

                // 5. Verificamos y mostramos el teclado
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                // Pequeñísimo retraso final por si el requestFocus necesita un ciclo de CPU
                etHiddenInput.postDelayed({
                    if (etHiddenInput.hasFocus()) {
                        imm.showSoftInput(etHiddenInput, InputMethodManager.SHOW_IMPLICIT)
                        Log.d("ModoReto_GLL", "ÉXITO al mostrar teclado: El EditText tiene el foco.")
                    } else {
                        Log.e("ModoReto_GLL", "FALLO FINAL: El EditText perdió el foco incluso después del GlobalLayoutListener.")
                    }
                }, 50)
            }
        })

        // El listener que lucha por el foco se mantiene, por si acaso
        etHiddenInput.setOnFocusChangeListener { _, hasFocus ->
            Log.d("ModoReto_Focus", "El foco del EditText ha cambiado a: $hasFocus")
            if (!hasFocus) {
                Log.w("ModoReto_Focus", "¡El foco se ha perdido! Intentando recuperarlo...")
                // No lo pedimos de nuevo aquí para evitar bucles,
                // dejaremos que el OnGlobalLayoutListener sea el único que inicie la acción.
            }
        }
    }

    private fun setupLetterBoxesWhenReady(palabra: String?) {
        // Usamos un ViewTreeObserver para esperar a que el layout esté listo
        layoutLetterBoxes.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Una vez que el layout ha sido medido, podemos obtener su ancho.
                // Es crucial remover el listener para que no se llame múltiples veces.
                layoutLetterBoxes.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Ahora sí, configuramos las cajas con el tamaño correcto
                configureBoxesWithSize(palabra)
            }
        })
    }

    private fun configureBoxesWithSize(palabra: String?) {
        // 1. Limpiar vistas anteriores
        layoutLetterBoxes.removeAllViews()
        letterBoxViews.clear()

        // 2. Obtener el ancho total disponible para el LinearLayout
        val containerWidth = layoutLetterBoxes.width
        Log.d("ModoReto_Debug", "Ancho del contenedor medido: $containerWidth")
        var letterCount = 0
        if(palabra!=null) {
            letterCount = palabra.length
        }
        // Dejamos un pequeño espacio entre cajas
        val spaceBetweenBoxes = 4 * (resources.displayMetrics.density).toInt() // 4dp en píxeles

        // 3. Calcular el tamaño máximo de cada caja
        // Ancho total menos los espacios, dividido por el número de letras
        var boxSize = (containerWidth - (spaceBetweenBoxes * (letterCount - 1))) / letterCount

        Log.d("ModoReto_Debug", "Ancho de la caja: $boxSize")

        // (Opcional) Establecer un tamaño máximo para palabras cortas para que no sean gigantes
        val maxBoxSize = (60 * resources.displayMetrics.density).toInt() // 60dp en píxeles
        if (boxSize > maxBoxSize) {
            boxSize = maxBoxSize
        }

        boxSize = (boxSize * 0.60f).toInt()

        Log.d("ModoReto_Debug", "Ancho de la cajadespues de la reduccion: $boxSize")

        // 4. Crear e inflar cada caja con el tamaño calculado
        if (palabra != null) {
            for (char in palabra) {
                val inflater = LayoutInflater.from(this)
                val letterBoxView = inflater.inflate(R.layout.item_letter_box, layoutLetterBoxes, false) as MaterialCardView
                val tvLetter = letterBoxView.findViewById<TextView>(R.id.tvLetter)

                // 5. ¡AQUÍ ESTÁ LA MAGIA! Asignar el tamaño dinámicamente
                val params = letterBoxView.layoutParams as ViewGroup.MarginLayoutParams
                params.width = boxSize
                params.height = (boxSize * 1.2).toInt() // Hacemos la altura un poco más grande que el ancho
                params.setMargins(0, 0, if (char != palabra.last()) spaceBetweenBoxes else 0, 0) // Añadir margen derecho a todas menos la última
                letterBoxView.layoutParams = params

                // (Opcional) Ajustar el tamaño del texto según el tamaño de la caja
                tvLetter.textSize = (boxSize / 4).toFloat()

                // 6. Añadir la vista configurada
                layoutLetterBoxes.addView(letterBoxView)
                letterBoxViews.add(tvLetter)
            }
        }
    }
    private fun setupDynamicLetterBoxes(palabra: String?) {
        // El Handler es una buena idea para esperar a que la vista se mida. Lo mantenemos.
        Handler(Looper.getMainLooper()).postDelayed({
            val measuredView = layoutLetterBoxes

            if (palabra.isNullOrEmpty()) {
                measuredView.removeAllViews()
                letterBoxViews.clear()
                Log.e("ModoReto", "Palabra nula o vacía, no se pueden crear cajas.")
                return@postDelayed
            }

            measuredView.removeAllViews()
            letterBoxViews.clear()

            // --- LÓGICA DE CÁLCULO (LA MANTENEMOS, ES CORRECTA) ---
            val availableWidth = (measuredView.width - measuredView.paddingLeft - measuredView.paddingRight).toFloat()
            val letterCount = palabra.length
            Log.d("ModoReto_Debug", "Ancho Total: ${measuredView.width}, Padding: ${measuredView.paddingLeft}, Ancho Disponible: $availableWidth")
            if (availableWidth <= 0 || letterCount == 0) return@postDelayed

            val spaceBetweenBoxes = (8 * resources.displayMetrics.density) // Aumenté un poco el espacio para que se note
            val boxSizeFloat = (availableWidth - (spaceBetweenBoxes * (letterCount - 1))) / letterCount
            var boxSize = floor(boxSizeFloat).toInt()
            val maxBoxSize = (60 * resources.displayMetrics.density).toInt()
            if (boxSize > maxBoxSize) {
                boxSize = maxBoxSize
            }

            boxSize = (boxSize * 0.80f).toInt()

            val minBoxSize = (36 * resources.displayMetrics.density).toInt() // 36dp como mínimo, por ejemplo
            if (boxSize < minBoxSize) {
                boxSize = minBoxSize
            }

            // 6. Crear e inflar cada caja y el separador
            for (i in 0 until letterCount) {
                // --- AÑADIR LA CAJA (MaterialCardView) ---
                val inflater = LayoutInflater.from(this)
                val letterBoxView = inflater.inflate(R.layout.item_letter_box, measuredView, false) as MaterialCardView
                val tvLetter = letterBoxView.findViewById<TextView>(R.id.tvLetter)

                // Obtenemos los params, pero NO tocaremos sus márgenes
                val params = letterBoxView.layoutParams as ViewGroup.MarginLayoutParams
                params.width = boxSize
                params.height = boxSize // O (boxSize * 1.2).toInt() si quieres rectángulos

                // !! IMPORTANTE: NO ESTABLECEMOS MÁRGENES AQUÍ !!
                // params.rightMargin = 0  <-- Asegúrate de que no quede ningún resto de esto

                letterBoxView.layoutParams = params
                tvLetter.textSize = (boxSize / 3.5f)

                measuredView.addView(letterBoxView)
                letterBoxViews.add(tvLetter)

                // --- AÑADIR EL SEPARADOR (Space) ---
                // Añadimos un Space explícito solo si NO es la última caja
                if (i < letterCount - 1) {
                    val separador = android.widget.Space(this)
                    // El alto es irrelevante, el ancho es nuestro espacio
                    separador.layoutParams = LinearLayout.LayoutParams(spaceBetweenBoxes.toInt(), 1)
                    measuredView.addView(separador)
                }
            }
        }, 100) // 50ms puede ser un poco justo, si sigue fallando prueba con 100ms
    }

    private fun setupInteraction() {    layoutLetterBoxes.setOnClickListener {
        Log.d("ModoReto_Interact", "CLICK: Despertando a etHiddenInput y pidiendo teclado.")

        // 1. Despertar: Hacemos que el EditText pueda recibir foco.
        etHiddenInput.isFocusable = true
        etHiddenInput.isFocusableInTouchMode = true

        // 2. Pedir: Solicitamos el foco explícitamente.
        //    Ahora somos los únicos pidiéndolo.
        etHiddenInput.requestFocus()

        // 3. Mostrar: Abrimos el teclado.
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etHiddenInput, InputMethodManager.SHOW_IMPLICIT)
    }
    }
}
