package com.example.edunova

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class ManageCategoriesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton

    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseConnection()
    private var currentSchool: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos un layout simple con lista y botón flotante
        setContentView(R.layout.activity_manage_categories)

        recyclerView = findViewById(R.id.rvCategories)
        fabAdd = findViewById(R.id.fabAddCategory)

        // Botón volver
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupListeners()
        loadTeacherDataAndCategories()
    }

    private fun setupListeners() {
        fabAdd.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun loadTeacherDataAndCategories() {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getTeacherSchool(currentUser.uid) { school ->
                if (school != null) {
                    currentSchool = school
                    loadCategories(school)
                } else {
                    Toast.makeText(this, "Error: Sin centro asignado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCategories(school: String) {
        db.collection("categorias")
            .whereEqualTo("school", school)
            .get()
            .addOnSuccessListener { documents ->
                val categories = documents.map { doc ->
                    CategoryItem(
                        id = doc.id,
                        name = doc.getString("nombre") ?: "",
                        school = school
                    )
                }
                // Configuramos el adaptador
                recyclerView.adapter = CategoryAdapter(categories, school) { category ->
                    deleteCategory(category)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar categorías", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "Nombre de la categoría (ej. Animales)"

        AlertDialog.Builder(this)
            .setTitle("Nueva Categoría")
            .setView(input) // En un caso real, usa un layout con márgenes
            .setPositiveButton("Añadir") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty() && currentSchool != null) {
                    addCategoryToFirebase(name)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addCategoryToFirebase(name: String) {
        val data = hashMapOf(
            "nombre" to name,
            "school" to currentSchool,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("categorias")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Categoría añadida", Toast.LENGTH_SHORT).show()
                loadCategories(currentSchool!!) // Recargar lista
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteCategory(category: CategoryItem) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Categoría")
            .setMessage("¿Borrar '${category.name}'? Los pictogramas asociados no se borrarán, pero perderán su categoría.")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("categorias").document(category.id).delete()
                    .addOnSuccessListener {
                        loadCategories(currentSchool!!)
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

// --- CLASES DE DATOS ---
data class CategoryItem(val id: String, val name: String, val school: String)
data class MiniPictogram(val name: String, val imageUrl: String)

// --- ADAPTADOR DE CATEGORÍAS (EXPANDIBLE CON ANIMACIÓN) ---
class CategoryAdapter(
    private val categories: List<CategoryItem>,
    private val school: String,
    private val onDelete: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvCategoryName)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteCategory)
        val rvPictograms: RecyclerView = v.findViewById(R.id.rvPictogramsInCategory)
        val layoutHeader: View = v.findViewById(R.id.layoutHeader)
        val ivArrow: ImageView = v.findViewById(R.id.ivExpandArrow) // Referencia a la flecha
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.name
        holder.btnDelete.setOnClickListener { onDelete(category) }

        // Configuración inicial del RecyclerView interno
        holder.rvPictograms.layoutManager = LinearLayoutManager(holder.itemView.context)

        // Estado inicial: Colapsado
        holder.rvPictograms.visibility = View.GONE
        holder.ivArrow.rotation = 0f // Flecha mirando abajo

        holder.layoutHeader.setOnClickListener {
            if (holder.rvPictograms.visibility == View.VISIBLE) {
                // COLAPSAR
                holder.rvPictograms.visibility = View.GONE
                // Animación de rotación a 0 grados (abajo)
                holder.ivArrow.animate().rotation(0f).setDuration(300).start()
            } else {
                // EXPANDIR
                holder.rvPictograms.visibility = View.VISIBLE
                // Animación de rotación a 180 grados (arriba)
                holder.ivArrow.animate().rotation(180f).setDuration(300).start()

                // Cargar datos (si no se han cargado ya, podrías optimizarlo aquí)
                loadPictogramsForCategory(category.name, holder.rvPictograms)
            }
        }
    }

    private fun loadPictogramsForCategory(categoryName: String, recyclerView: RecyclerView) {
        val db = FirebaseFirestore.getInstance()
        db.collection("palabras")
            .whereEqualTo("school", school)
            .whereEqualTo("categoria", categoryName)
            .get()
            .addOnSuccessListener { documents ->
                val pictos = documents.map {
                    MiniPictogram(
                        name = it.getString("palabra") ?: "",
                        imageUrl = it.getString("urlImagen") ?: ""
                    )
                }
                recyclerView.adapter = MiniPictogramAdapter(pictos)
            }
    }

    override fun getItemCount() = categories.size
}

// --- ADAPTADOR INTERNO (MINI PICTOGRAMAS) ---
class MiniPictogramAdapter(private val pictos: List<MiniPictogram>) :
    RecyclerView.Adapter<MiniPictogramAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.ivMiniPictogram)
        val tv: TextView = v.findViewById(R.id.tvPictogramName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mini_pictogram, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pictos[position]
        holder.tv.text = item.name
        holder.iv.load(item.imageUrl) {
            placeholder(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount() = pictos.size
}