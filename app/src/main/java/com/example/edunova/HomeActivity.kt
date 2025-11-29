package com.example.edunova

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import com.example.edunova.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // private val db: FirebaseFirestore = FirebaseFirestore.getInstance() // No se usa aquí, se puede quitar o dejar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Recuperar el rol del Intent
        val userRole = intent.getStringExtra("USER_ROLE")

        // 2. Configurar la interfaz según el rol
        setupRoleUI(userRole)

        setupClickListeners()
    }

    private fun setupRoleUI(role: String?) {
        Log.d("HomeActivity", "--- DEBUG ROL ---")
        Log.d("HomeActivity", "Rol recibido del Intent: '$role'")

        if (role == "teacher") {
            // CONFIRMACIÓN DE PROFESOR
            Log.d("HomeActivity", ">> Configurando vista de PROFESOR")

            binding.btnAdminPanel.visibility = android.view.View.VISIBLE
            binding.badgeTeacher.visibility = android.view.View.VISIBLE

            binding.btnAdminPanel.setOnClickListener {
                Log.d("HomeActivity", "Click en botón Admin -> Yendo a ProfesorActivity")
                val intent = Intent(this, ProfesorActivity::class.java)
                startActivity(intent)
            }
        } else {
            // CONFIRMACIÓN DE ALUMNO
            Log.d("HomeActivity", ">> Configurando vista de ALUMNO")
            binding.btnAdminPanel.visibility = android.view.View.GONE
            binding.badgeTeacher.visibility = android.view.View.GONE
        }
    }

    private fun setupClickListeners() {

        // JUEGO VOCABULARIO (Palabras)
        binding.cardLearn.setOnClickListener {
            val intent = Intent(this, JuegoPalabras::class.java)
            startActivity(intent)
        }

        // JUEGO SÍLABAS
        binding.cardGuess.setOnClickListener {
            val intent = Intent(this, SilabasActivity::class.java)
            startActivity(intent)
        }

        // --- CAMBIO AQUÍ: CONEXIÓN CON EL JUEGO DE FRASES ---
        binding.cardBuild.setOnClickListener {
            val intent = Intent(this, JuegoFrasesActivity::class.java)
            startActivity(intent)
            // showToast("Modo: Construir Frases") // Ya no es necesario el mensaje
        }

        // RETO DIARIO
        binding.cardChallenge.setOnClickListener {
            val intent = Intent(this, RetoActivity::class.java)
            startActivity(intent)
        }

        // AJUSTES
        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // CERRAR SESIÓN
        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            showToast("Sesión cerrada")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}