package com.example.edunova

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// Imports de Firebase eliminados
import com.example.edunova.databinding.ActivityRegisterBinding
import com.example.edunova.db.FirebaseConnection // <-- 1. Importa la nueva clase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    // private lateinit var auth: FirebaseAuth // <-- 2. Eliminado
    // private lateinit var db: FirebaseFirestore // <-- 2. Eliminado
    private lateinit var repository: FirebaseConnection // <-- 3. Añade la variable del repositorio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


        repository = FirebaseConnection() // <-- 5. Inicializa el repositorio

        binding.buttonRegister.setOnClickListener {
            if (validateFields()) {
                registerUser()
            }
        }
        binding.textViewGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateFields(): Boolean {

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
        return true
    }

    // <-- 6. El método registerUser() ahora es mucho más simple
    private fun registerUser() {
        // Muestra la ProgressBar y oculta el botón
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonRegister.visibility = View.INVISIBLE

        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val name = binding.editTextName.text.toString().trim()

        // Llama al repositorio y espera la respuesta en el callback
        repository.registerUser(name, email, password) { success, message ->
            // Este código se ejecutará cuando el repositorio termine.

            // Oculta la ProgressBar y muestra el botón de nuevo
            binding.progressBar.visibility = View.GONE
            binding.buttonRegister.visibility = View.VISIBLE

            if (success) {
                // Registro exitoso
                Toast.makeText(this, "Registro completado con éxito.", Toast.LENGTH_LONG).show()

                // TODO: Navegar a la pantalla principal (HomeActivity)
                // val intent = Intent(this, HomeActivity::class.java)
                // startActivity(intent)
                // finishAffinity()
            } else {
                // Si el registro falla, muestra el mensaje de error del repositorio
                Toast.makeText(
                    baseContext,
                    "Fallo en el registro: $message",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}