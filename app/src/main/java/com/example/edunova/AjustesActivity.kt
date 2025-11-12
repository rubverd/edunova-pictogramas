package com.example.edunova // Asegúrate de que este sea tu paquete

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityAjustesBinding // Importa la clase de ViewBinding

class AjustesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout usando ViewBinding
        binding = ActivityAjustesBinding.inflate(layoutInflater)
        // Establece la vista de la actividad con la raíz del binding
        setContentView(binding.root)

        // --- PASOS CLAVE PARA LA TOOLBAR ---

        // 1. Establece la Toolbar que definiste en tu XML como la ActionBar oficial de la actividad.
        setSupportActionBar(binding.toolbarAjustes)

        // 2. Muestra el botón de "Atrás" (la flecha) en la ActionBar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // 3. Gestiona el evento de clic en el botón "Atrás".
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Comprueba si el botón pulsado es el de "Atrás" (su ID es android.R.id.home).
        if (item.itemId == android.R.id.home) {
            // Cierra la actividad actual y vuelve a la anterior en la pila de navegación.
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
