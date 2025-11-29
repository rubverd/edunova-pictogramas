package com.example.edunova

import android.os.Bundle
import android.view.View
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

class AddFraseActivity : AppCompatActivity() {

    // --- 1. DECLARACIÓN DE VARIABLES ---
    private lateinit var ivPreview: ImageView
    private lateinit var etImageUrl: TextInputEditText
    private lateinit var etWord: TextInputEditText
    private lateinit var etFrase: TextInputEditText
    private lateinit var sliderDifficulty: Slider
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: Button

    private val repository = FirebaseConnection()

    // Variable para almacenar el centro del profesor (Puede ser nula al principio)
    private var currentSchool: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_frase)

        // --- 2. INICIALIZACIÓN ---
        ivPreview = findViewById(R.id.ivPreview)
        etImageUrl = findViewById(R.id.etImageUrl)
        etFrase = findViewById(R.id.etFrase)
        sliderDifficulty = findViewById(R.id.sliderDifficulty)
        progressBar = findViewById(R.id.progressBar)
        btnSave = findViewById(R.id.btnSave)

        val btnPreviewImage = findViewById<Button>(R.id.btnPreviewImage)
        val btnBack = findViewById<ImageButton>(R.id.returnButton)

        // 1. Botón Volver
        btnBack.setOnClickListener {
            finish()
        }

        // 2. Cargar el Centro del Profesor al iniciar
        btnSave.isEnabled = false

        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getTeacherSchool(currentUser.uid) { school ->
                if (school != null) {
                    currentSchool = school
                    btnSave.isEnabled = true
                } else {
                    Toast.makeText(this, "Error: No tienes centro asignado.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        // 3. Botón Ver Imagen
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

        // 4. Botón Guardar
        btnSave.setOnClickListener {
            if (validateFields()) {
                saveDataToFirestore()
            }
        }
    }

    // --- 4. LÓGICA ---

    private fun validateFields(): Boolean {
        if (etImageUrl.text.isNullOrEmpty()) {
            etImageUrl.error = "Falta la URL de la imagen"
            return false
        }
        if (etWord.text.isNullOrEmpty()) {
            etWord.error = "Falta la palabra"
            return false
        }
        if (etFrase.text.isNullOrEmpty()) {
            etFrase.error = "Faltan las palabras"
            return false
        }
        // Validación extra de seguridad
        if (currentSchool == null) {
            Toast.makeText(this, "No se ha podido identificar tu centro", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveDataToFirestore() {
        setLoading(true)

        val urlImagen = etImageUrl.text.toString().trim()
        val dificultad = sliderDifficulty.value.toInt()

        val rawFrase = etFrase.text.toString().trim()
        val listaPalabras = rawFrase.split("-").map { it.trim() }

        // Mapa de datos
        // CORRECCIÓN AQUÍ: Usamos (currentSchool ?: "") para asegurar que no es nulo
        val pictogramData = hashMapOf(
            "dificultad" to dificultad,
            "palabras" to listaPalabras,
            "urlImagen" to urlImagen,
            "updatedAt" to System.currentTimeMillis(),
            "school" to (currentSchool ?: "") // <-- ESTO SOLUCIONA EL ERROR DE TIPOS
        )

        repository.savePictogram(pictogramData) { success ->
            setLoading(false)
            if (success) {
                Toast.makeText(this, "¡Pictograma guardado en $currentSchool!", Toast.LENGTH_SHORT)
                    .show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
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