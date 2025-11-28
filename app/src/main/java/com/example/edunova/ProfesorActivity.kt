package com.example.edunova

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfesorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profesor)

        // 1. Ver lista de Alumnos
        val btnStudents = findViewById<Button>(R.id.btnViewStudents)
        btnStudents.setOnClickListener {
            val intent = Intent(this, ListStudentsActivity::class.java)
            startActivity(intent)
        }

        // 2. Añadir Pictograma (Formulario)
        val btnAddWord = findViewById<Button>(R.id.btnAddWord)
        btnAddWord.setOnClickListener {
            val intent = Intent(this, AddPictogramActivity::class.java)
            startActivity(intent)
        }

        // 3. Crear Nivel (Pendiente)
        val btnLevels = findViewById<Button>(R.id.btnCreateLevel)
        btnLevels.setOnClickListener {
            val intent = Intent(this, AddFraseActivity::class.java)
            startActivity(intent)
        }

        // 4. Volver
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }
}