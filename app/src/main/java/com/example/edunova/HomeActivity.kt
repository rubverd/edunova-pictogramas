package com.example.edunova

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import coil.load
import com.example.edunova.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.edunova.db.FirebaseConnection


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar los listeners para cada tarjeta de modo de juego
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardLearn.setOnClickListener {
            val intent = Intent(this, JuegoPalabras::class.java)

            // Inicia la nueva actividad
            startActivity(intent)
        }

        binding.cardGuess.setOnClickListener {
            // TODO: Reemplazar con: startActivity(Intent(this, GuessActivity::class.java))
            val intent= Intent(this, SilabasActivity::class.java)
            startActivity(intent)

            showToast("Modo: Adivinar")
        }

        binding.cardBuild.setOnClickListener {
            // TODO: Reemplazar con: startActivity(Intent(this, BuildSentenceActivity::class.java))
            showToast("Modo: Construir Frases")
        }

        binding.cardChallenge.setOnClickListener {
            // TODO: Reemplazar con: startActivity(Intent(this, ChallengeActivity::class.java))
            showToast("Modo: Reto Diario")
        }

        // Listeners para los botones de la barra inferior
        binding.buttonSettings.setOnClickListener {
            // TODO: Reemplazar con: startActivity(Intent(this, SettingsActivity::class.java))
            startActivity(Intent(this, SettingsActivity::class.java))
            //showToast("Ajustes")
        }

        binding.buttonLogout.setOnClickListener {
            // Lógica para cerrar sesión en Firebase
            auth.signOut()

            // Redirigir al usuario a la pantalla de Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            // Estas flags limpian el historial para que el usuario no pueda volver al Home
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            showToast("Sesión cerrada")
        }
    }

    // Función de ayuda para mostrar mensajes Toast rápidamente
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
