package com.example.edunova

import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edunova.databinding.ActivityRegisterBinding
import com.example.edunova.db.FirebaseConnection

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var repository: FirebaseConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = FirebaseConnection()

        // 1. Iniciamos la lógica para ocultar/mostrar el campo de clase según el rol
        setupRoleSelection()

        binding.buttonRegister.setOnClickListener {
            if (validateFields()) {
                registerUser()
            }
        }
        binding.textViewGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupRoleSelection() {
        // Escuchamos cambios en el RadioGroup
        binding.radioGroupRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbTeacher.id) {
                // Si es PROFESOR: Ocultamos la clase (Gone)
                binding.textFieldLayoutClass.visibility = View.GONE
            } else {
                // Si es ALUMNO: Mostramos la clase (Visible)
                binding.textFieldLayoutClass.visibility = View.VISIBLE
            }
        }
    }

    private fun validateFields(): Boolean {
        // Limpiar errores previos
        binding.textFieldLayoutName.error = null
        binding.textFieldLayoutEmail.error = null
        binding.textFieldLayoutPassword.error = null
        binding.textFieldLayoutSchool.error = null
        binding.textFieldLayoutClass.error = null

        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val school = binding.editTextSchool.text.toString().trim()
        val classroom = binding.editTextClass.text.toString().trim()

        var isValid = true

        // Validaciones estándar
        if (name.isEmpty()) {
            binding.textFieldLayoutName.error = "El nombre es obligatorio"
            isValid = false
        }
        if (email.isEmpty()) {
            binding.textFieldLayoutEmail.error = "El correo es obligatorio"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textFieldLayoutEmail.error = "Correo no válido"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.textFieldLayoutPassword.error = "La contraseña es obligatoria"
            isValid = false
        } else if (password.length < 6) {
            binding.textFieldLayoutPassword.error = "Mínimo 6 caracteres"
            isValid = false
        }

        // NUEVAS validaciones
        if (school.isEmpty()) {
            binding.textFieldLayoutSchool.error = "El colegio es obligatorio"
            isValid = false
        }

        // Solo validamos la clase si está marcado "Alumno"
        if (binding.rbStudent.isChecked && classroom.isEmpty()) {
            binding.textFieldLayoutClass.error = "La clase es obligatoria para alumnos"
            isValid = false
        }

        return isValid
    }

    private fun registerUser() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonRegister.visibility = View.INVISIBLE

        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val school = binding.editTextSchool.text.toString().trim()
        val classroom = binding.editTextClass.text.toString().trim()

        // 1. Preparamos los datos extra
        val additionalData = HashMap<String, Any>()

        additionalData["school"] = school // El colegio va para todos

        if (binding.rbTeacher.isChecked) {
            // Caso PROFESOR
            additionalData["role"] = "teacher"
        } else {
            // Caso ALUMNO
            additionalData["role"] = "student"
            additionalData["classroom"] = classroom
            // Opcional: additionalData["assignedTeacherId"] = ""
        }

        // 2. Llamamos al repositorio pasando el mapa de datos
        Log.d("Registro", "Iniciando llamada al repositorio...") // <--- CHIVATO 1

        repository.registerUser(name, email, password, additionalData) { success, message ->

            Log.d("Registro", "Callback recibido. Success: $success, Mensaje: $message") // <--- CHIVATO 2

            // Ocultar carga
            binding.progressBar.visibility = View.GONE
            binding.buttonRegister.visibility = View.VISIBLE

            if (success) {
                Log.d("Registro", "Intentando abrir MainActivity...") // <--- CHIVATO 3
                Toast.makeText(this, "Registro completado.", Toast.LENGTH_LONG).show()

                try {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("Registro", "Error al cambiar de pantalla", e) // <--- Si falla aquí, lo veremos
                }
            } else {
                Toast.makeText(baseContext, "Error: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
}