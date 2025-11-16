package com.example.edunova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityMainBinding
import com.example.edunova.db.FirebaseConnection // Importamos tu repositorio

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 1. Sustituimos las instancias directas de Firebase por tu repositorio
    private lateinit var repository: FirebaseConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Inicializamos el repositorio
        repository = FirebaseConnection()

        // Configuración del botón Login
        binding.buttonLogin.setOnClickListener {
            // Usamos binding para obtener los textos, es más limpio
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            // Validaciones básicas de UI
            when {
                email.isEmpty() || password.isEmpty() ->
                    binding.textEstado.text = "Completa todos los campos."

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    binding.textEstado.text = "Correo no válido."

                password.length < 6 ->
                    binding.textEstado.text = "La contraseña debe tener al menos 6 caracteres."

                else -> {
                    // Si todo es correcto, llamamos al método de login
                    binding.textEstado.text = "" // Limpiar errores previos
                    performLogin(email, password)
                }
            }
        }

        // Configuración del enlace a Registro
        binding.textViewGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // 3. Método privado que usa tu FirebaseConnection
    private fun performLogin(email: String, password: String) {
        // Opcional: Mostrar un ProgressBar si lo tienes en el layout
        // binding.progressBar.visibility = View.VISIBLE
        // binding.buttonLogin.isEnabled = false

        Log.d("MainActivity", "Intentando login con: $email")

        // Llamada asíncrona al repositorio
        repository.loginUser(email, password) { success, message ->

            // Opcional: Ocultar ProgressBar
            // binding.progressBar.visibility = View.GONE
            // binding.buttonLogin.isEnabled = true

            if (success) {
                Log.d("MainActivity", "Login exitoso")

                // Aquí es donde, en el siguiente paso, comprobaremos si es PROFE o ALUMNO.
                // De momento, lo dejamos como estaba, yendo a HomeActivity.
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish() // Cerramos el login para que no pueda volver atrás con "Atrás"
            } else {
                // Error en el login
                Log.e("MainActivity", "Error login: $message")
                binding.textEstado.text = "Error: $message"
                Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
            }
        }
    }
}