package com.example.edunova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityLoginBinding
import com.example.edunova.databinding.ActivityMainBinding
import com.example.edunova.db.FirebaseConnection // Importamos tu repositorio

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // 1. Sustituimos las instancias directas de Firebase por tu repositorio
    private lateinit var repository: FirebaseConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
        // Opcional: Mostrar loading...

        repository.loginUser(email, password) { success, message ->
            if (success) {
                // 1. Obtenemos el usuario actual de Firebase Auth
                val currentUser = repository.getCurrentUser()

                if (currentUser != null) {
                    // 2. Consultamos a Firestore para saber su ROL
                    repository.getUserRole(currentUser.uid) { role ->
                        Log.d("MainActivity", "Rol detectado: $role")

                        // 3. Vamos al Home pasando el rol
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.putExtra("USER_ROLE", role) // "teacher" o "student"
                        startActivity(intent)
                        finish()
                    }
                }
            } else {
                Log.e("MainActivity", "Error login: $message")
                binding.textEstado.text = "Error: $message"
                Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
            }
        }
    }
}