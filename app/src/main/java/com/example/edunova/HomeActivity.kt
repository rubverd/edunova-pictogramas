package com.example.edunova

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
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
            val userRole = intent.getStringExtra("USER_ROLE")
            if(userRole != "teacher") {
                checkStudentProgressAndStartChallenge()
            } else {
                startActivity(Intent(this, RetoActivity::class.java))
            }
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

    private fun checkStudentProgressAndStartChallenge() {
        val userId = auth.currentUser?.uid
        if (userId == null){
            showToast("Error: No se pudo obtener el ID del usuario")
            return
        }
        val db = FirebaseFirestore.getInstance()
        val progressDocRef = db.collection("userProgress").document(userId)
        progressDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val completedPalabras = document.getBoolean("completedPalabras") == true

                    if (completedPalabras) {
                        Log.d("HomeActivity", "Progreso completado, puede iniciar el reto.")
                        startActivity(Intent(this, RetoActivity::class.java))
                    } else {
                        Log.d("HomeActivity", "Progreso incompleto, no puede iniciar el reto.")
                        showAccessDeniedDialog()
                    }
                } else {
                    Log.d("HomeActivity", "No se encontró el documento de progreso del usuario.")
                    showAccessDeniedDialog()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeActivity", "Error al obtener el progreso del usuairo", exception)
                showToast("No se pudo verificar tu progrso. Intentalo de nuevo.")
            }
    }

    /**
     * Muestra un dialogo informativo que indica que modos se deben completar primero
     */
    private fun showAccessDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Aun no puedes acceder!")
            .setMessage("Para desbloquear el modo reto debes completar los otros modos primero.")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_lock)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}