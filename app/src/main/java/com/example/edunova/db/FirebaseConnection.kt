package com.example.edunova.db

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
// Nota: Se elimina FirebaseStorage ya que no se usa de momento

/**
 * Repositorio general para manejar las operaciones de Firebase.
 * Esta clase centraliza toda la funcionalidad de la base de datos
 * (Posteriormente se separarán funcionalidades)
 */
class FirebaseConnection {

    // Instancias de Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Devuelve la instancia de Firestore
     */
    fun getFirestoreInstance(): FirebaseFirestore {
        return db
    }

    // --- MÉTODOS DE AUTENTICACIÓN ---

    /**
     * Intenta registrar un nuevo usuario en Firebase Auth y guardar sus datos en Firestore.
     */
    fun registerUser(
        name: String,
        email: String,
        pass: String,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        // 1. Crear el usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // 2. Si Auth fue exitoso, guardar info adicional en Firestore
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        saveUserInfoToFirestore(firebaseUser.uid, name, email, onComplete)
                    } else {
                        onComplete(false, "No se pudo obtener el UID del usuario.")
                    }
                } else {
                    onComplete(false, authTask.exception?.message ?: "Error en el registro.")
                }
            }
    }

    /**
     * Intenta iniciar sesión con un usuario existente.
     */
    fun loginUser(
        email: String,
        pass: String,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Error al iniciar sesión.")
                }
            }
    }

    /**
     * Obtiene el usuario actualmente logueado
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // --- MÉTODOS DE FIRESTORE (DATOS DE USUARIO) ---

    /**
     * Guarda la información adicional del usuario en la colección "usuarios".
     * Esta función es privada porque solo debe ser llamada desde este repositorio.
     */
    private fun saveUserInfoToFirestore(
        uid: String,
        name: String,
        email: String,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        val userInfo = hashMapOf(
            "name" to name,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("usuarios").document(uid)
            .set(userInfo)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, "Error al guardar la información del usuario: ${e.message}")
            }
    }

    // --- (Aquí puedes añadir más métodos para imágenes, ejercicios, etc. más adelante) ---

}