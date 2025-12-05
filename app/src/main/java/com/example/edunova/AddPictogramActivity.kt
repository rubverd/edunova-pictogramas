package com.example.edunova

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.edunova.db.FirebaseConnection
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AddPictogramActivity : AppCompatActivity() {

    // --- 1. DECLARACIÓN DE VARIABLES ---
    private lateinit var ivPreview: ImageView
    private lateinit var etImageUrl: TextInputEditText
    private lateinit var etWord: TextInputEditText
    private lateinit var etSyllables: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var sliderDifficulty: Slider
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: Button

    private val repository = FirebaseConnection()
    private val db = FirebaseFirestore.getInstance()

    // Variables de control
    private var currentSchool: String? = null
    private var existingCategories = mutableListOf<String>()

    // VARIABLE NUEVA: Para saber si estamos editando (si es null, estamos creando)
    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pictogram)

        // --- 2. INICIALIZACIÓN ---
        ivPreview = findViewById(R.id.ivPreview)
        etImageUrl = findViewById(R.id.etImageUrl)
        etWord = findViewById(R.id.etWord)
        etSyllables = findViewById(R.id.etSyllables)
        actvCategory = findViewById(R.id.actvCategory) // AutoCompleteTextView
        sliderDifficulty = findViewById(R.id.sliderDifficulty)
        progressBar = findViewById(R.id.progressBar)
        btnSave = findViewById(R.id.btnSave)

        val btnPreviewImage = findViewById<Button>(R.id.btnPreviewImage)
        val btnBack = findViewById<ImageButton>(R.id.returnButton)

        // Botón volver
        btnBack.setOnClickListener { finish() }

        // Cargar datos del profesor
        btnSave.isEnabled = false
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getTeacherSchool(currentUser.uid) { school ->
                if (school != null) {
                    currentSchool = school
                    btnSave.isEnabled = true
                    // Cargamos las categorías del colegio para el desplegable
                    loadCategoriesForDropdown(school)
                } else {
                    Toast.makeText(this, "Error: No tienes centro asignado.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Botón Ver Imagen (Preview)
        btnPreviewImage.setOnClickListener {
            val url = etImageUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                Glide.with(this)
                    .load(url)
                    .placeholder(android.R.drawable.ic_menu_upload)
                    .error(android.R.drawable.ic_delete)
                    .into(ivPreview)
            } else {
                Toast.makeText(this, "Introduce una URL primero", Toast.LENGTH_SHORT).show()
            }
        }

        // --- COMPROBAR SI ESTAMOS EN MODO EDICIÓN ---
        checkForEditMode()

        // Botón Guardar
        btnSave.setOnClickListener {
            if (validateFields()) {
                saveDataToFirestore()
            }
        }
    }

    // --- NUEVO: Rellenar datos si venimos de "Editar" ---
    private fun checkForEditMode() {
        if (intent.hasExtra("EXTRA_ID")) {
            editingId = intent.getStringExtra("EXTRA_ID")

            // Cambiamos el texto del botón para dar feedback
            btnSave.text = "Actualizar Pictograma"

            // Rellenamos los campos con los datos recibidos
            etImageUrl.setText(intent.getStringExtra("EXTRA_IMAGE"))
            etWord.setText(intent.getStringExtra("EXTRA_WORD"))
            etSyllables.setText(intent.getStringExtra("EXTRA_SYLLABLES"))
            actvCategory.setText(intent.getStringExtra("EXTRA_CATEGORY"))

            // Forzamos que el AutoComplete oculte el menú al rellenarse
            actvCategory.dismissDropDown()

            val diff = intent.getIntExtra("EXTRA_DIFFICULTY", 1)
            sliderDifficulty.value = diff.toFloat()

            // Cargar preview de imagen automáticamente
            val url = intent.getStringExtra("EXTRA_IMAGE")
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).into(ivPreview)
            }
        }
    }

    private fun loadCategoriesForDropdown(school: String) {
        db.collection("categorias")
            .whereEqualTo("school", school)
            .get()
            .addOnSuccessListener { documents ->
                existingCategories.clear()
                for (doc in documents) {
                    doc.getString("nombre")?.let { existingCategories.add(it) }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, existingCategories)
                actvCategory.setAdapter(adapter)

                // Mostrar desplegable al tocar
                actvCategory.setOnClickListener { actvCategory.showDropDown() }
            }
    }

    private fun validateFields(): Boolean {
        if (etImageUrl.text.isNullOrEmpty()) {
            etImageUrl.error = "Falta la URL"
            return false
        }
        if (etWord.text.isNullOrEmpty()) {
            etWord.error = "Falta la palabra"
            return false
        }
        if (actvCategory.text.isNullOrEmpty()) {
            actvCategory.error = "Falta la categoría"
            return false
        }
        if (currentSchool == null) return false
        return true
    }

    private fun saveDataToFirestore() {
        setLoading(true)

        // Recogemos datos
        val pictogramData = hashMapOf(
            "palabra" to etWord.text.toString().trim(),
            "categoria" to actvCategory.text.toString().trim(),
            "dificultad" to sliderDifficulty.value.toInt(),
            "silabas" to etSyllables.text.toString().trim().split("-").map { it.trim() },
            "urlImagen" to etImageUrl.text.toString().trim(),
            "updatedAt" to System.currentTimeMillis(),
            "school" to (currentSchool ?: "")
        )

        // Guardamos también la categoría si es nueva
        checkAndSaveCategory(actvCategory.text.toString().trim())

        if (editingId != null) {
            // --- MODO EDICIÓN: Sobrescribimos el documento existente (SET) ---
            db.collection("palabras").document(editingId!!)
                .set(pictogramData)
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(this, "¡Actualizado correctamente!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    setLoading(false)
                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
        } else {
            // --- MODO CREACIÓN: Creamos uno nuevo (ADD) ---
            repository.savePictogram(pictogramData) { success ->
                setLoading(false)
                if (success) {
                    Toast.makeText(this, "¡Pictograma guardado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAndSaveCategory(categoryName: String) {
        if (!existingCategories.contains(categoryName)) {
            val newCategoryData = hashMapOf(
                "nombre" to categoryName,
                "school" to (currentSchool ?: ""),
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("categorias").add(newCategoryData)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnSave.isEnabled = true
        }
    }
}