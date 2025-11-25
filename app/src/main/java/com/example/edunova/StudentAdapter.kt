package com.example.edunova

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edunova.clases.Student

// 1. CAMBIO CLAVE: Añadimos 'onStudentClick' al constructor aquí
class StudentAdapter(
    private val students: List<Student>,
    private val onStudentClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvStudentName)
        val tvClass: TextView = view.findViewById(R.id.tvStudentClass)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.tvName.text = student.displayName

        if (student.classroom.isNotEmpty()) {
            holder.tvClass.text = "Clase: ${student.classroom}"
        } else {
            holder.tvClass.text = "Sin clase asignada"
        }

        // 2. CAMBIO CLAVE: Usamos la función al hacer clic
        holder.itemView.setOnClickListener {
            onStudentClick(student)
        }
    }

    override fun getItemCount() = students.size
}