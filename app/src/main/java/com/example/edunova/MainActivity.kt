package com.example.edunova

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val textEstado = findViewById<TextView>(R.id.textEstado)

        buttonLogin.setOnClickListener {
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
                    textEstado.text = "Inicio de sesión exitoso."
            }
        }
    }
}
