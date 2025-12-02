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

    // --- VARIABLES ---
    private lateinit var ivPreview: ImageView
    private lateinit var etImageUrl: TextInputEditText
    private lateinit var etFrase: TextInputEditText
    private lateinit var sliderDifficulty: Slider
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: Button
    private lateinit var btnPreviewImage: Button
    private lateinit var btnBack: ImageButton

    private val repository = FirebaseConnection()

    // VARIABLE NUEVA: Para almacenar el centro
    private var currentSchool: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_frase)

        // --- INICIALIZACIÓN DE VISTAS ---
        ivPreview = findViewById(R.id.ivPreview)
        etImageUrl = findViewById(R.id.etImageUrl)
        etFrase = findViewById(R.id.etFrase)
        sliderDifficulty = findViewById(R.id.sliderDifficulty)
        progressBar = findViewById(R.id.progressBar)
        btnSave = findViewById(R.id.btnSave)
        btnPreviewImage = findViewById(R.id.btnPreviewImage)
        btnBack = findViewById(R.id.returnButton)

        // Deshabilitamos guardar hasta cargar el centro (Seguridad)
        btnSave.isEnabled = false

        setupListeners()
        loadTeacherData()
    }

    private fun loadTeacherData() {
        // Cargar el Centro del Profesor al iniciar (Lógica traída de AddPictogramActivity)
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getTeacherSchool(currentUser.uid) { school ->
                if (school != null) {
                    currentSchool = school
                    btnSave.isEnabled = true // Habilitamos botón ahora que tenemos centro
                } else {
                    Toast.makeText(this, "Error: No tienes centro asignado.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

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

        btnSave.setOnClickListener {
            if (validateFields()) {
                saveDataToFirestore()
            }
        }
    }

    private fun validateFields(): Boolean {
        if (etImageUrl.text.isNullOrEmpty()) {
            etImageUrl.error = "Falta la URL de la imagen"
            return false
        }
        if (etFrase.text.isNullOrEmpty()) {
            etFrase.error = "Introduce la frase completa"
            return false
        }
        if (currentSchool == null) {
            Toast.makeText(this, "No se ha podido identificar tu centro", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveDataToFirestore() {
        setLoading(true)

        val urlImagen = etImageUrl.text.toString().trim()
        val fraseCompleta = etFrase.text.toString().trim()
        val dificultad = sliderDifficulty.value.toInt()
        val escuela = currentSchool ?: "" // Usamos la variable del centro

        // Pasamos la escuela al método actualizado
        repository.savePhrase(fraseCompleta, urlImagen, dificultad, escuela) { success ->
            setLoading(false)
            if (success) {
                Toast.makeText(this, "¡Frase guardada en $escuela!", Toast.LENGTH_SHORT).show()
                limpiarCampos()
            } else {
                Toast.makeText(this, "Error al guardar la frase", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun limpiarCampos() {
        etFrase.text?.clear()
        etImageUrl.text?.clear()
        sliderDifficulty.value = 1f
        ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false
            etFrase.isEnabled = false
            etImageUrl.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnSave.isEnabled = true
            etFrase.isEnabled = true
            etImageUrl.isEnabled = true
        }
    }
}