package com.example.edunova

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.util.Log
import android.content.ContentValues.TAG
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Para obtener la instancia de la base de datos
import com.google.firebase.auth.auth
import com.example.edunova.databinding.ActivityMainBinding
import android.content.Intent



class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var resultadoTextView: TextView
    private lateinit var auth: FirebaseAuth

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        db = FirebaseFirestore.getInstance()

        auth = Firebase.auth

        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val textEstado = findViewById<TextView>(R.id.textEstado)

        binding.buttonLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            when {
                email.isEmpty() || password.isEmpty() ->
                    textEstado.text = "Completa todos los campos."

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    textEstado.text = "Correo no válido."

                password.length < 6 ->
                    textEstado.text = "La contraseña debe tener al menos 6 caracteres."

                else ->
                    iniciarSesion(email, password)
            }
        }


        // 3. Configura el listener para el TextView que funciona como enlace
        binding.textViewGoToRegister.setOnClickListener {
            // Crea un Intent para ir de MainActivity a RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)

            // Inicia la nueva actividad
            startActivity(intent)
        }
    }


    private fun iniciarSesion(email: String, password: String) {
        Log.d("DEBUG_AUTH", "--- Iniciando intento de login ---")
        Log.d("DEBUG_AUTH", "Email Limpio (después de trim): '$email'")
        Log.d("DEBUG_AUTH", "Contraseña enviada: '$password'")

        // 3. Imprime las longitudes. ¡Esto es CRUCIAL!
        Log.d("DEBUG_AUTH", "Longitud Email Original: ${email.length}")
        Log.d("DEBUG_AUTH", "Longitud Contraseña: ${password.length}")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // ¡Inicio de sesión exitoso!
                    Log.d("Auth", "signInWithEmail:success")
                    val user = auth.currentUser // Obtienes el usuario autenticado

                    // Ahora que el usuario está autenticado, puedes buscar sus datos en Firestore
                    // usando su ID único (user.uid), que es más seguro que el email.


                } else {
                    // Si el inicio de sesión falla, se muestra un mensaje al usuario.
                    Log.w("Auth", "signInWithEmail:failure", task.exception)
                    // Aquí podrías mostrar un Toast o un error en la UI
                }
            }
    }

}
