package com.example.edunova

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MiApplication : Application() {

    override fun onCreate() {super.onCreate()

        // Leer la preferencia del tema al iniciar la app
        val prefs = getSharedPreferences("AjustesPrefs", MODE_PRIVATE)
        val modoOscuroActivado = prefs.getBoolean("modo_oscuro_activado", false)

        // Aplicar el tema guardado
        if (modoOscuroActivado) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
