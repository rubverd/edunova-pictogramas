package com.example.edunova

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfesorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profesor)

        // 1. Configurar el botón "Mis Alumnos"
        val btnStudents = findViewById<Button>(R.id.btnViewStudents)
        btnStudents.setOnClickListener {
            // Aquí pondremos la lógica para abrir la lista de alumnos
            Toast.makeText(this, "Próximamente: Lista de Alumnos", Toast.LENGTH_SHORT).show()
        }

        // 2. Configurar el botón "Añadir Pictograma"
        val btnAddWord = findViewById<Button>(R.id.btnAddWord)
        btnAddWord.setOnClickListener {
            Toast.makeText(this, "Próximamente: Añadir Palabra", Toast.LENGTH_SHORT).show()
        }

        // 3. Configurar el botón "Gestionar Niveles"
        val btnLevels = findViewById<Button>(R.id.btnCreateLevel)
        btnLevels.setOnClickListener {
            Toast.makeText(this, "Próximamente: Crear Nivel", Toast.LENGTH_SHORT).show()
        }

        // 4. Configurar el botón "Volver"
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Cierra esta actividad y vuelve a la anterior (HomeActivity)
        }
    }
}