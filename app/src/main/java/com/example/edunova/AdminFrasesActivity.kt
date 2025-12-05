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
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminFrasesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCreate: MaterialButton
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_frases)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnCreate = findViewById(R.id.btnCreatePhrase)
        recyclerView = findViewById(R.id.rvPhrases)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Crear nueva frase (sin datos extra)
        btnCreate.setOnClickListener {
            val intent = Intent(this, AddFraseActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarFrases()
    }

    private fun cargarFrases() {
        db.collection("frases")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val list = documents.map { doc ->
                    PhraseItem(
                        id = doc.id,
                        text = doc.getString("frase") ?: "",
                        imageUrl = doc.getString("urlImagen") ?: "",
                        difficulty = doc.getLong("dificultad")?.toInt() ?: 1
                    )
                }
                // Pasamos dos lambdas: una para borrar y otra para editar
                recyclerView.adapter = PhrasesAdapter(list,
                    onDeleteClick = { phrase -> confirmingBorrado(phrase) },
                    onEditClick = { phrase -> editPhrase(phrase) }
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar frases", Toast.LENGTH_SHORT).show()
            }
    }

    // --- FUNCIÓN EDITAR ---
    private fun editPhrase(item: PhraseItem) {
        val intent = Intent(this, AddFraseActivity::class.java)
        // Pasamos los datos existentes para rellenar el formulario
        intent.putExtra("EXTRA_ID", item.id)
        intent.putExtra("EXTRA_FRASE", item.text)
        intent.putExtra("EXTRA_IMAGE", item.imageUrl)
        intent.putExtra("EXTRA_DIFFICULTY", item.difficulty)
        startActivity(intent)
    }

    private fun confirmingBorrado(phrase: PhraseItem) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar frase")
            .setMessage("¿Borrar: '${phrase.text}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("frases").document(phrase.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Frase eliminada", Toast.LENGTH_SHORT).show()
                        cargarFrases()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- DATA CLASS ---
    data class PhraseItem(val id: String, val text: String, val imageUrl: String, val difficulty: Int)

    // --- ADAPTER ---
    class PhrasesAdapter(
        private val phrases: List<PhraseItem>,
        private val onDeleteClick: (PhraseItem) -> Unit,
        private val onEditClick: (PhraseItem) -> Unit // Nuevo callback
    ) : RecyclerView.Adapter<PhrasesAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivThumb: ImageView = v.findViewById(R.id.ivPhraseThumb)
            val tvText: TextView = v.findViewById(R.id.tvPhraseText)
            val tvDiff: TextView = v.findViewById(R.id.tvDifficulty)
            val btnDel: ImageButton = v.findViewById(R.id.btnDelete)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit) // Nuevo botón
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_frase, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = phrases[position]
            holder.tvText.text = item.text
            holder.tvDiff.text = "Nivel: ${item.difficulty}"

            holder.ivThumb.load(item.imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }

            holder.btnDel.setOnClickListener { onDeleteClick(item) }
            holder.btnEdit.setOnClickListener { onEditClick(item) }
        }

        override fun getItemCount() = phrases.size
    }
}