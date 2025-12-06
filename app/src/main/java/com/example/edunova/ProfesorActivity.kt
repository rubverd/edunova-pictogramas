package com.example.edunova

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ProfesorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profesor)

        // 1. Alumnos
        findViewById<Button>(R.id.btnViewStudents).setOnClickListener {
            startActivity(Intent(this, ListStudentsActivity::class.java))
        }

        // 2. Categorías
        findViewById<Button>(R.id.btnManageCategories).setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }

        // 3. GESTIONAR PICTOGRAMAS (Nuevo enlace)
        findViewById<Button>(R.id.btnManagePictograms).setOnClickListener {
            startActivity(Intent(this, ManagePictogramsActivity::class.java))
        }

        // 4. Frases
        findViewById<Button>(R.id.btnManagePhrases).setOnClickListener {
            startActivity(Intent(this, AdminFrasesActivity::class.java))
        }

        // 5. Volver
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}