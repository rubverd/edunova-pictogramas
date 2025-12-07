package com.example.edunova // ¡Asegúrate de que este sea tu paquete correcto!

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.edunova.databinding.ActivityAjustesBinding // Usa tu clase de ViewBinding

class AjustesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustesBinding
    private lateinit var sharedPreferences: SharedPreferences

    // Claves para guardar y recuperar la preferencia del modo oscuro.
    // Usar constantes evita errores de escritura.
    companion object {
        const val PREFS_NAME = "settings_prefs"
        const val KEY_NIGHT_MODE = "night_mode_preference"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                Log.d("ModoOscuroCheck", "El modo actual es CLARO (NO)")
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                Log.d("ModoOscuroCheck", "El modo actual es OSCURO (YES)")
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                Log.d("ModoOscuroCheck", "El modo actual es INDEFINIDO")
            }
        }



        binding = ActivityAjustesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa SharedPreferences para poder guardar la configuración
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Configura la barra de herramientas (Toolbar)
        setSupportActionBar(binding.toolbarAjustes)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAjustes.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Botón de volver
        }

        // Configura el switch del modo oscuro
        setupDarkModeSwitch()
    }

    private fun setupDarkModeSwitch() {
        val switchModoOscuro = binding.switchModoOscuro

        // Lee la preferencia guardada. El valor por defecto es 'seguir al sistema'.
        val savedMode = sharedPreferences.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // Ajusta el estado del switch según la preferencia guardada
        switchModoOscuro.isChecked = when (savedMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true  // Si está forzado a oscuro, el switch está activado.
            AppCompatDelegate.MODE_NIGHT_NO -> false // Si está forzado a claro, el switch está desactivado.
            else -> {
                // Si sigue al sistema, el estado del switch debe reflejar el estado actual del teléfono.
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        // Añade el listener para reaccionar cuando el usuario pulsa el switch
        switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            // Determina qué modo aplicar y guardar
            val newMode = if (isChecked) {
                // Si el switch se activa, forzamos el modo oscuro
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                // Si se desactiva, forzamos el modo claro
                AppCompatDelegate.MODE_NIGHT_NO
            }

            // 1. Aplica el nuevo modo a toda la aplicación.
            // Esto recreará la actividad para que los cambios de tema surtan efecto.
            AppCompatDelegate.setDefaultNightMode(newMode)

            // 2. Guarda la preferencia del usuario para que se recuerde en futuros inicios.
            sharedPreferences.edit().putInt(KEY_NIGHT_MODE, newMode).apply()
        }
    }
}
