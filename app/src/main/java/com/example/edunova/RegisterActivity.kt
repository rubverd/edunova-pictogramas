package com.example.edunova

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.example.edunova.databinding.ActivityRegisterBinding // ¡Importante! Habilita ViewBinding

class RegisterActivity : AppCompatActivity() {

    // 1. Declara las variables para ViewBinding y Firebase Auth
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Infla el layout usando ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Inicializa Firebase Auth
        auth = Firebase.auth

        // 4. Configura el listener para el botón de registro
        binding.buttonRegister.setOnClickListener {
            if (validateFields()) {
                // Si la validación es correcta, intenta registrar al usuario
                registerUser()
            }
        }
        binding.textViewGoToLogin.setOnClickListener {
            // Esta acción simplemente finaliza la actividad actual (RegisterActivity)
            finish()
        }
    }

    private fun validateFields(): Boolean {
        // Limpia los errores previos
        binding.textFieldLayoutName.error = null
        binding.textFieldLayoutEmail.error = null
        binding.textFieldLayoutPassword.error = null

        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (name.isEmpty()) {
            binding.textFieldLayoutName.error = "El nombre es obligatorio"
            return false
        }

        if (email.isEmpty()) {
            binding.textFieldLayoutEmail.error = "El correo es obligatorio"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textFieldLayoutEmail.error = "Correo no válido"
            return false
        }

        if (password.isEmpty()) {
            binding.textFieldLayoutPassword.error = "La contraseña es obligatoria"
            return false
        }

        if (password.length < 6) {
            binding.textFieldLayoutPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }

        // Si todas las validaciones pasan
        return true
    }

    private fun registerUser() {
        // Muestra la ProgressBar y oculta el botón
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonRegister.visibility = View.INVISIBLE

        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Oculta la ProgressBar y muestra el botón de nuevo
                binding.progressBar.visibility = View.GONE
                binding.buttonRegister.visibility = View.VISIBLE

                if (task.isSuccessful) {
                    // Registro exitoso
                    Toast.makeText(this, "Registro exitoso.", Toast.LENGTH_SHORT).show()

                    // Aquí iría la lógica para guardar el nombre en Firestore
                    // y luego navegar a la MainActivity
                    // ej: guardarDatosAdicionales()
                    //     startActivity(Intent(this, MainActivity::class.java))
                    //     finish()
                } else {
                    // Si el registro falla, muestra un mensaje al usuario.
                    Toast.makeText(
                        baseContext,
                        "Fallo en el registro: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
