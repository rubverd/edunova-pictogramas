package com.example.edunova

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityAjustesBinding // ¡Importante! Asegúrate de que el nombre del binding sea correcto.

class SettingsActivity : AppCompatActivity() {

    // Declara una variable para el ViewBinding.
    // El nombre 'ActivityAjustesBinding' se genera a partir de 'activity_ajustes.xml'.
    private lateinit var binding: ActivityAjustesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Infla el layout usando la clase de binding generada.
        binding = ActivityAjustesBinding.inflate(layoutInflater)

        // 2. Establece el contenido de la actividad a la vista raíz del layout inflado.
        setContentView(binding.root)

        // --- (Opcional pero recomendado) Configurar la Toolbar para el botón de "Atrás" ---

        // Establece la Toolbar que definiste en tu XML como la ActionBar oficial.
        setSupportActionBar(binding.toolbarAjustes)

        // Muestra el botón de "Atrás" (la flecha) en la ActionBar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // Gestiona el evento de clic en el botón "Atrás" de la Toolbar.
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Cierra la actividad actual y vuelve a la anterior.
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
