package com.example.edunova

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log // Importa la clase Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityAjustesBinding

class AjustesActivity : AppCompatActivity() {

    // Companion object para constantes, similar a 'static final' en Java.
    companion object {
        const val PREFS_NAME = "AjustesPrefs"
        const val KEY_VELOCIDAD = "velocidad_reproduccion"
        const val VELOCIDAD_LENTA = 0.5f
        const val VELOCIDAD_NORMAL = 1.0f
        const val VELOCIDAD_RAPIDA = 1.5f
    }

    // Usamos View Binding para acceder a las vistas de forma segura y eficiente.
    private lateinit var binding: ActivityAjustesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout usando View Binding y lo establece como el contenido de la actividad.
        binding = ActivityAjustesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Configurar la Toolbar
        setSupportActionBar(binding.toolbarAjustes)
        // Habilita el botón de "atrás" en la Toolbar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 2. Cargar la configuración guardada y actualizar la UI
        cargarPreferenciaVelocidad()

        // 3. Configurar el listener para guardar la selección del usuario
        binding.toggleButtonVelocidad.addOnButtonCheckedListener { group, checkedId, isChecked ->
            // Solo reaccionamos cuando un botón es seleccionado (isChecked es true)
            if (isChecked) {
                val velocidadSeleccionada = when (checkedId) {
                    R.id.btnVelocidadLenta -> VELOCIDAD_LENTA
                    R.id.btnVelocidadNormal -> VELOCIDAD_NORMAL
                    R.id.btnVelocidadRapida -> VELOCIDAD_RAPIDA
                    else -> VELOCIDAD_NORMAL // Valor por defecto en caso de error
                }
                guardarPreferenciaVelocidad(velocidadSeleccionada)
            }
        }
    }

    /**
     * Lee la velocidad guardada en SharedPreferences y actualiza el botón que
     * debe aparecer como seleccionado en la interfaz.
     */
    private fun cargarPreferenciaVelocidad() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        // Por defecto, la velocidad será normal (1.0f) si no hay nada guardado.
        val velocidadGuardada = prefs.getFloat(KEY_VELOCIDAD, VELOCIDAD_NORMAL)

        // --- LOG --- Añadimos un log para ver qué valor se está cargando.
        Log.d("AjustesActivity", "Preferencia CARGADA - Velocidad: $velocidadGuardada")

        // Usamos 'when' para una sintaxis más limpia.
        val botonAChequear = when (velocidadGuardada) {
            VELOCIDAD_LENTA -> R.id.btnVelocidadLenta
            VELOCIDAD_RAPIDA -> R.id.btnVelocidadRapida
            else -> R.id.btnVelocidadNormal // Cubre VELOCIDAD_NORMAL y casos inesperados.
        }
        binding.toggleButtonVelocidad.check(botonAChequear)
    }

    /**
     * Guarda el valor de la velocidad seleccionada en SharedPreferences.
     * @param velocidad El valor float (0.5f, 1.0f, o 1.5f) a guardar.
     */
    private fun guardarPreferenciaVelocidad(velocidad: Float) {
        // --- LOG --- Añadimos un log para ver qué valor se va a guardar.
        Log.d("AjustesActivity", "Preferencia GUARDANDO - Velocidad: $velocidad")

        val editor: SharedPreferences.Editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
        editor.putFloat(KEY_VELOCIDAD, velocidad)
        editor.apply() // apply() guarda los datos en segundo plano de forma asíncrona.
    }

    /**
     * Maneja el evento de clic en los elementos de la Toolbar, como el botón "atrás".
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Si se pulsa el botón de "atrás" (home), finaliza la actividad.
        if (item.itemId == android.R.id.home) {
            finish() // Cierra la actividad actual y vuelve a la anterior.
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

