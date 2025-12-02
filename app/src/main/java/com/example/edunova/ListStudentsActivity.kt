package com.example.edunova

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edunova.db.FirebaseConnection

class ListStudentsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSchoolName: TextView
    private lateinit var repository: FirebaseConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de que el nombre del XML aquí coincida exactamente con el nombre del archivo en layout
        setContentView(R.layout.activity_list_students)

        // Inicializar Vistas
        recyclerView = findViewById(R.id.rvStudents)
        progressBar = findViewById(R.id.progressBarStudents)
        tvSchoolName = findViewById(R.id.tvSchoolName)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializar Repositorio
        repository = FirebaseConnection()
        val currentUser = repository.getCurrentUser()

        val btnBack = findViewById<ImageButton>(R.id.returnButton) // Si en el XML se llama "returnButton"
        btnBack.setOnClickListener {
            finish() // Cierra la actividad y vuelve atrás
        }
        // Lógica de carga
        if (currentUser != null) {
            loadTeacherSchool(currentUser.uid)
        } else {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTeacherSchool(uid: String) {
        val db = repository.getFirestoreInstance()

        // 1. Consultamos el perfil del PROFESOR para saber su colegio
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val school = document.getString("school")
                    if (!school.isNullOrEmpty()) {
                        tvSchoolName.text = "Centro: $school"
                        loadStudents(school) // 2. Cargamos alumnos de ese centro
                    } else {
                        progressBar.visibility = View.GONE
                        tvSchoolName.text = "Sin centro asignado"
                        Toast.makeText(this, "No tienes un centro asignado.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar perfil.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadStudents(school: String) {
        repository.getStudentsBySchool(school) { students ->
            progressBar.visibility = View.GONE

            if (students.isEmpty()) {
                Toast.makeText(this, "No hay alumnos en este centro.", Toast.LENGTH_SHORT).show()
            } else {
                // 3. Pasamos la lambda para manejar el clic
                recyclerView.adapter = StudentAdapter(students) { studentClicked ->
                    // Esto se ejecuta cuando tocas un alumno
                    val intent = Intent(this, StudentProgressActivity::class.java)
                    intent.putExtra("STUDENT_UID", studentClicked.uid) // Pasamos el ID
                    intent.putExtra("STUDENT_NAME", studentClicked.displayName) // Pasamos el Nombre
                    startActivity(intent)
                }
            }
        }
    }
}