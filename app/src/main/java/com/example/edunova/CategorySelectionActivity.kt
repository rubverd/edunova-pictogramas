package com.example.edunova

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore

class CategorySelectionActivity : AppCompatActivity() {

    private lateinit var progressBar: View
    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_selection)

        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.rvCategories)

        // Configuramos Grid de 2 columnas para que parezcan tarjetas grandes
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        findViewById<View>(R.id.toolbar).setOnClickListener { finish() }

        loadStudentDataAndCategories()
    }

    private fun loadStudentDataAndCategories() {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getUserData(currentUser.uid) { data ->
                val school = data?.get("school") as? String
                if (!school.isNullOrEmpty()) {
                    loadCategories(school)
                } else {
                    Toast.makeText(this, "Error: No tienes centro asignado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun loadCategories(school: String) {
        db.collection("categorias")
            .whereEqualTo("school", school)
            .get()
            .addOnSuccessListener { documents ->
                val categories = mutableListOf<String>()

                // 1. Añadimos siempre la opción "Aleatorio" primero
                categories.add("Aleatorio")

                // 2. Añadimos las categorías reales
                for (doc in documents) {
                    doc.getString("nombre")?.let { categories.add(it) }
                }

                progressBar.visibility = View.GONE

                recyclerView.adapter = CategorySelectionAdapter(categories) { selectedCategory ->
                    startGame(selectedCategory)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar categorías", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startGame(category: String) {
        val intent = Intent(this, JuegoPalabras::class.java)
        intent.putExtra("EXTRA_CATEGORY", category)
        startActivity(intent)
        finish() // Cerramos selección para que al volver del juego vaya al menú principal
    }

    // --- ADAPTER INTERNO ---
    class CategorySelectionAdapter(
        private val categories: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvCategoryName)
            val card: MaterialCardView = v.findViewById(R.id.cardCategory)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Usamos un layout simple para la tarjeta
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_selection, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.tvName.text = category

            // Color especial para "Aleatorio"
            if (category == "Aleatorio") {
                holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.Mustard)) // O un color destacado
            } else {
                holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.white))
            }

            holder.itemView.setOnClickListener { onClick(category) }
        }

        override fun getItemCount() = categories.size
    }
}