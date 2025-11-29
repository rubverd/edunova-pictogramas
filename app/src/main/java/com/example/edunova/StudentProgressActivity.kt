package com.example.edunova

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edunova.clases.StudentAttempt
import com.example.edunova.db.FirebaseConnection

class StudentProgressActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var tvEmpty: View
    private val repository = FirebaseConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_progress)

        // Recuperar datos del intent
        val studentUid = intent.getStringExtra("STUDENT_UID") ?: ""
        val studentName = intent.getStringExtra("STUDENT_NAME") ?: "Alumno"

        // Configurar UI
        findViewById<TextView>(R.id.tvStudentTitle).text = "Progreso de $studentName"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        recyclerView = findViewById(R.id.rvProgress)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (studentUid.isNotEmpty()) {
            loadAttempts(studentUid)
        }
    }

    private fun loadAttempts(uid: String) {
        repository.getStudentAttempts(uid) { attempts ->
            progressBar.visibility = View.GONE
            if (attempts.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
            } else {
                recyclerView.adapter = AttemptsAdapter(attempts)
            }
        }
    }

    // --- ADAPTADOR INTERNO PARA LA LISTA DE RESULTADOS ---
    class AttemptsAdapter(private val attempts: List<StudentAttempt>) :
        RecyclerView.Adapter<AttemptsAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvType: TextView = v.findViewById(android.R.id.text1)
            val tvInfo: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Usamos un layout estándar que permite dos líneas de texto
            val v = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = attempts[position]

            // Línea 1: Tipo de ejercicio y Fecha
            holder.tvType.text = "${item.exerciseType.uppercase()}  -  ${item.getDateString()}"
            holder.tvType.setTypeface(null, android.graphics.Typeface.BOLD)

            // Línea 2: Nota y Tiempo
            // Usamos los helpers que creamos en el Paso 1
            val textoInfo = "Aciertos: ${item.getScoreString()}   |   Tiempo: ${item.getDurationString()} min"
            holder.tvInfo.text = textoInfo
            holder.tvInfo.setTextColor(android.graphics.Color.DKGRAY)
        }

        override fun getItemCount() = attempts.size
    }
}