package com.example.edunova

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        // 2. NUEVO: Gestionar Categorías
        val btnCategories = findViewById<Button>(R.id.btnManageCategories)
        btnCategories.setOnClickListener {
            val intent = Intent(this, ManageCategoriesActivity::class.java)
            startActivity(intent)
        }

        // 3. Añadir Pictograma
        val btnAddWord = findViewById<Button>(R.id.btnAddWord)
        btnAddWord.setOnClickListener {
            val intent = Intent(this, AddPictogramActivity::class.java)
            startActivity(intent)
        }

        // 4. Gestionar Frases
        val btnManage = findViewById<Button>(R.id.btnManagePhrases)
        btnManage.setOnClickListener {
            val intent = Intent(this, AdminFrasesActivity::class.java)
            startActivity(intent)
        }

        // 5. Volver
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }
}