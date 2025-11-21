package com.example.edunova

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.edunova.databinding.SilabasBinding
import java.util.Locale
import android.content.Intent
import android.widget.Button
import com.google.android.material.appbar.MaterialToolbar


class SilabasActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: SilabasBinding
    private lateinit var letterMap: Map<Char, TextView>
    private lateinit var tts: TextToSpeech

    // Esta parte está bien con by lazy, ya que la carga es diferida.
// En SilabasActivity.kt

    private val gruposDeSilabasOrdenados: List<List<String>> by lazy {
        val silabas = listOf(
            "ba", "be", "bi", "bo", "bu", "ca", "ce", "ci", "co", "cu",
            "da", "de", "di", "do", "du", "fa", "fe", "fi", "fo", "fu",
            "ga", "ge", "gi", "go", "gu", "gui", "gue", "ha", "he", "hi", "ho", "hu",
            "ja", "je", "ji", "jo", "ju", "ka", "ke", "ki", "ko", "ku",
            "la", "le", "li", "lo", "lu", "ma", "me", "mi", "mo", "mu",
            "na", "ne", "ni", "no", "nu", "ña", "ñe", "ñi", "ño", "ñu", "pa", "pe", "pi", "po", "pu",
            "que", "qui", "ra", "re", "ri", "ro", "ru", "sa", "se", "si", "so", "su",
            "ta", "te", "ti", "to", "tu", "va", "ve", "vi", "vo", "vu",
            "wa", "we", "wi", "wo", "wu", "xa", "xe", "xi", "xo", "xu",
            "ya", "ye", "yi", "yo", "yu", "za", "ze", "zi", "zo", "zu"
        )

        // --- LÓGICA DE ORDENAMIENTO CORREGIDA ---
        // 1. Obtener un Collator para el idioma español.
        val collator = java.text.Collator.getInstance(Locale("es", "ES"))
        collator.strength = java.text.Collator.PRIMARY // Ignora mayúsculas/minúsculas y acentos si fuera necesario

        // 2. Ordenar la lista de sílabas usando el Collator.
        val silabasOrdenadas = silabas.sortedWith(compareBy(collator) { it })

        // 3. Agrupar por la primera letra y devolver la lista de grupos.
        silabasOrdenadas.groupBy { it.first() }.values.toList()
    }

    private var silabaActual: String? = null
    private var indiceGrupoActual: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SilabasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa el motor TTS aquí, pero el juego comenzará después en onInit
        tts = TextToSpeech(this, this)

        // Las inicializaciones rápidas se quedan aquí
        initializeLetterMap()

        val botonVolver = findViewById<MaterialToolbar>(R.id.toolbar)
        botonVolver.setOnClickListener {
            // Crea un Intent para ir de MainActivity a RegisterActivity
            finish()
        }

        binding.buttonOption3.setOnClickListener {
            val respuestaUsuario = binding.respuesta.text.toString().trim()
            if (silabaActual != null) {
                verificarRespuesta(respuestaUsuario)
            }
            avanzarAlSiguienteGrupo()
        }

        binding.fabPlaySoundSilabas.setOnClickListener {
            reproducirSonido(binding.TextoSilabas.text.toString())
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

    // ... (El resto de tus funciones como reproducirSonido, avanzarAlSiguienteGrupo, etc., se quedan igual)


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
        binding.buttonOption3.isEnabled = true
        avanzarAlSiguienteGrupo()
    }

    private fun avanzarAlSiguienteGrupo() {
        indiceGrupoActual++
        // La comprobación del final del juego ya no es necesaria aquí.
        // Solo avanzamos si no hemos llegado al final.
        if (indiceGrupoActual < gruposDeSilabasOrdenados.size) {
            val grupoActual = gruposDeSilabasOrdenados[indiceGrupoActual]
            silabaActual = grupoActual.randomOrNull()
            binding.TextoSilabas.text = silabaActual ?: "Error"
        }
    }


    private fun verificarRespuesta(respuesta: String) {
        if (respuesta.equals(silabaActual, ignoreCase = true)) {
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()

            // --- Marcar la letra como correcta (tu código actual está bien) ---
            val primeraSilabaDelGrupo = gruposDeSilabasOrdenados[indiceGrupoActual].firstOrNull()
            val letra = primeraSilabaDelGrupo?.firstOrNull()
            if (letra != null) {
                val textViewDeLetra = letterMap[letra.uppercaseChar()]
                textViewDeLetra?.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_correcto)
            }


            if (indiceGrupoActual >= gruposDeSilabasOrdenados.size - 1) {
                // Si es el último grupo, mostramos el diálogo de inmediato
                binding.TextoSilabas.text = "¡Fin!"
                binding.buttonOption3.isEnabled = false // Desactivamos el botón de avanzar
                mostrarDialogoCompletado()
            }

        } else {
            Toast.makeText(this, "Incorrecto. La sílaba era '$silabaActual'", Toast.LENGTH_SHORT).show()
        }
        binding.respuesta.text?.clear()
    }


    private fun initializeLetterMap() {
        letterMap = mapOf(
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
            'Ñ' to binding.NEne,
            'P' to binding.P,
            'Q' to binding.Q,
            'R' to binding.letraR,
            'S' to binding.S,
            'T' to binding.T,
            'V' to binding.V,
            'W' to binding.W,
            'X' to binding.X,
            'Y' to binding.Y,
            'Z' to binding.Z
        )
    }

    override fun onDestroy() {
        // No olvides liberar los recursos de TTS cuando la actividad se destruya
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun mostrarDialogoCompletado() {
        // 1. Inflar la vista personalizada
        val dialogView = layoutInflater.inflate(R.layout.dialog_completado, null)

        // 2. Localizar los botones DENTRO de esa vista inflada
        val botonRepetir = dialogView.findViewById<Button>(R.id.boton_repetir)
        val botonVolverMenu = dialogView.findViewById<Button>(R.id.boton_volver_menu)

        // 3. Crear el diálogo usando el constructor
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)  // Establece la vista personalizada
            .setCancelable(false) // Impide que se cierre al tocar fuera
            .create()             // Crea el objeto AlertDialog

        // 4. (Opcional) Aplicar fondo personalizado
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // 5. Asignar las acciones a los botones
        botonRepetir.setOnClickListener {
            alertDialog.dismiss() // Cierra el diálogo
            reiniciarActividad()   // Llama a la función para reiniciar
        }

        botonVolverMenu.setOnClickListener {
            alertDialog.dismiss() // Cierra el diálogo
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Cierra SilabasActivity
        }

        // 6. Mostrar el diálogo
        alertDialog.show()
    }




    private fun reiniciarActividad() {
        // 1. Reiniciar los colores de fondo de todas las letras
        letterMap.values.forEach { textView ->
            textView.backgroundTintList = ContextCompat.getColorStateList(this, R.color.CherryBlossomPink)
        }

        // 2. Reiniciar el recorrido desde el principio
        iniciarRecorrido()
    }

}
