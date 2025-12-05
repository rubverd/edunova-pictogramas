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
import com.google.firebase.firestore.FirebaseFirestore

class AddFraseActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var etImageUrl: TextInputEditText
    private lateinit var etFrase: TextInputEditText
    private lateinit var sliderDifficulty: Slider
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: Button
    private lateinit var btnPreviewImage: Button
    private lateinit var btnBack: ImageButton

    private val repository = FirebaseConnection()
    private val db = FirebaseFirestore.getInstance() // Necesario para updates directos

    private var currentSchool: String? = null

    // Variable para saber si editamos
    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_frase)

        ivPreview = findViewById(R.id.ivPreview)
        etImageUrl = findViewById(R.id.etImageUrl)
        etFrase = findViewById(R.id.etFrase)
        sliderDifficulty = findViewById(R.id.sliderDifficulty)
        progressBar = findViewById(R.id.progressBar)
        btnSave = findViewById(R.id.btnSave)
        btnPreviewImage = findViewById(R.id.btnPreviewImage)
        btnBack = findViewById(R.id.returnButton)

        btnSave.isEnabled = false

        setupListeners()
        loadTeacherData()

        // Comprobar si venimos a editar
        checkForEditMode()
    }

    private fun checkForEditMode() {
        if (intent.hasExtra("EXTRA_ID")) {
            editingId = intent.getStringExtra("EXTRA_ID")

            // Cambiar textos visuales
            findViewById<android.widget.TextView>(R.id.tvAppTitle).text = "EDITAR FRASE"
            btnSave.text = "Actualizar Frase"

            // Rellenar campos
            etFrase.setText(intent.getStringExtra("EXTRA_FRASE"))
            etImageUrl.setText(intent.getStringExtra("EXTRA_IMAGE"))

            val diff = intent.getIntExtra("EXTRA_DIFFICULTY", 1)
            sliderDifficulty.value = diff.toFloat()

            // Cargar imagen
            val url = intent.getStringExtra("EXTRA_IMAGE")
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).into(ivPreview)
            }
        }
    }

    private fun loadTeacherData() {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            repository.getTeacherSchool(currentUser.uid) { school ->
                if (school != null) {
                    currentSchool = school
                    btnSave.isEnabled = true
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
            }
        }

        btnSave.setOnClickListener {
            if (validateFields()) {
                saveDataToFirestore()
            }
        }
    }

    private fun validateFields(): Boolean {
        if (etImageUrl.text.isNullOrEmpty()) return false
        if (etFrase.text.isNullOrEmpty()) return false
        if (currentSchool == null) return false
        return true
    }

    private fun saveDataToFirestore() {
        setLoading(true)

        val urlImagen = etImageUrl.text.toString().trim()
        val fraseCompleta = etFrase.text.toString().trim()
        val dificultad = sliderDifficulty.value.toInt()
        val escuela = currentSchool ?: ""

        // Separamos palabras (lógica crítica para el juego)
        val palabrasSeparadas = fraseCompleta.split("\\s+".toRegex())

        // Mapa de datos
        val data = hashMapOf(
            "frase" to fraseCompleta,
            "urlImagen" to urlImagen,
            "dificultad" to dificultad,
            "school" to escuela,
            "palabras" to palabrasSeparadas, // Importante actualizar esto también
            "updatedAt" to System.currentTimeMillis()
        )
        // Solo añadimos createdAt si es nuevo, para no perder la fecha original al editar
        if (editingId == null) {
            data["createdAt"] = System.currentTimeMillis()
        }

        if (editingId != null) {
            // --- MODO EDICIÓN (Actualizar) ---
            db.collection("frases").document(editingId!!)
                .update(data) // Usamos update o set(data, SetOptions.merge())
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(this, "Frase actualizada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    setLoading(false)
                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
        } else {
            // --- MODO CREACIÓN (Nuevo) ---
            // Usamos la función del repositorio que ya tenías
            repository.savePhrase(fraseCompleta, urlImagen, dificultad, escuela) { success ->
                setLoading(false)
                if (success) {
                    Toast.makeText(this, "¡Frase guardada!", Toast.LENGTH_SHORT).show()
                    finish() // Cerramos para volver a la lista
                } else {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
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