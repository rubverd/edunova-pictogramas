package com.example.edunova

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
// IMPORTANTE: Aquí importamos tu modelo desde su nueva carpeta
import com.example.edunova.clases.Student

class StudentAdapter(private val students: List<Student>) :
    RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvStudentName)
        val tvClass: TextView = view.findViewById(R.id.tvStudentClass)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        // Asegúrate de que R.layout.item_student existe en tus layouts
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvName.text = student.displayName

        // Mostrar clase o texto por defecto
        if (student.classroom.isNotEmpty()) {
            holder.tvClass.text = "Clase: ${student.classroom}"
        } else {
            holder.tvClass.text = "Sin clase asignada"
        }
    }

    override fun getItemCount() = students.size
}