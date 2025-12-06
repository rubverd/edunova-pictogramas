package com.example.edunova

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ManagePictogramsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseConnection()
    private var currentSchool: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_pictograms)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Botón "Crear Nuevo" (Abre la actividad de añadir vacía)
        findViewById<MaterialButton>(R.id.btnCreatePictogram).setOnClickListener {
            startActivity(Intent(this, AddPictogramActivity::class.java))
        }

        recyclerView = findViewById(R.id.rvPictograms)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadTeacherData()
    }

    override fun onResume() {
        super.onResume()
        if (currentSchool != null) loadPictograms()
    }

    private fun loadTeacherData() {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getTeacherSchool(currentUser.uid) { school ->
                if (school != null) {
                    currentSchool = school
                    loadPictograms()
                }
            }
        }
    }

    private fun loadPictograms() {
        db.collection("palabras")
            .whereEqualTo("school", currentSchool)
            .get()
            .addOnSuccessListener { documents ->
                val list = documents.map { doc ->
                    PictogramItem(
                        id = doc.id,
                        word = doc.getString("palabra") ?: "",
                        imageUrl = doc.getString("urlImagen") ?: "",
                        category = doc.getString("categoria") ?: "Sin categoría",
                        difficulty = doc.getLong("dificultad")?.toInt() ?: 1,
                        syllables = (doc.get("silabas") as? List<String>)?.joinToString("-") ?: ""
                    )
                }
                recyclerView.adapter = PictogramsAdapter(list,
                    onDelete = { deletePictogram(it) },
                    onEdit = { editPictogram(it) } // Pasamos función de editar
                )
            }
    }

    private fun deletePictogram(item: PictogramItem) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Borrar el pictograma '${item.word}'?")
            .setPositiveButton("Sí") { _, _ ->
                db.collection("palabras").document(item.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show()
                        loadPictograms()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun editPictogram(item: PictogramItem) {
        val intent = Intent(this, AddPictogramActivity::class.java)
        // Pasamos todos los datos para rellenar el formulario
        intent.putExtra("EXTRA_ID", item.id)
        intent.putExtra("EXTRA_WORD", item.word)
        intent.putExtra("EXTRA_IMAGE", item.imageUrl)
        intent.putExtra("EXTRA_CATEGORY", item.category)
        intent.putExtra("EXTRA_DIFFICULTY", item.difficulty)
        intent.putExtra("EXTRA_SYLLABLES", item.syllables)
        startActivity(intent)
    }

    // --- DATA CLASS & ADAPTER ---
    data class PictogramItem(
        val id: String, val word: String, val imageUrl: String,
        val category: String, val difficulty: Int, val syllables: String
    )

    class PictogramsAdapter(
        private val items: List<PictogramItem>,
        private val onDelete: (PictogramItem) -> Unit,
        private val onEdit: (PictogramItem) -> Unit
    ) : RecyclerView.Adapter<PictogramsAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val iv: ImageView = v.findViewById(R.id.ivPictoThumb)
            val tvWord: TextView = v.findViewById(R.id.tvPictoWord)
            val tvCat: TextView = v.findViewById(R.id.tvPictoCategory)
            val btnDel: ImageButton = v.findViewById(R.id.btnDelete)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_manage_pictogram, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvWord.text = item.word
            holder.tvCat.text = "${item.category} (Nivel ${item.difficulty})"
            holder.iv.load(item.imageUrl) { placeholder(android.R.drawable.ic_menu_gallery) }

            holder.btnDel.setOnClickListener { onDelete(item) }
            holder.btnEdit.setOnClickListener { onEdit(item) }
        }

        override fun getItemCount() = items.size
    }
}