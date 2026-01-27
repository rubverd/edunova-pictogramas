package com.example.edunova

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityCreditsBinding

class CreditsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreditsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar el View Binding
        binding = ActivityCreditsBinding.inflate(layoutInflater)

        // 2. Establecer la vista raíz en la actividad
        setContentView(binding.root)

        // Aquí puedes agregar lógica adicional, por ejemplo:
        // binding.textViewTitulo.text = "Créditos"
    }
}