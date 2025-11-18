package com.example.edunova.db

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
// Importamos tu clase Student desde la carpeta correcta
import com.example.edunova.clases.Student

class FirebaseConnection {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getFirestoreInstance(): FirebaseFirestore {
        return db
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // --- AUTENTICACIÓN ---

    fun loginUser(email: String, pass: String, onComplete: (success: Boolean, message: String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Error al iniciar sesión.")
                }
            }
    }

    fun registerUser(
        name: String,
        email: String,
        pass: String,
        additionalData: Map<String, Any>,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        saveUserInfoToFirestore(firebaseUser.uid, name, email, additionalData, onComplete)
                    } else {
                        onComplete(false, "No se pudo obtener el UID.")
                    }
                } else {
                    onComplete(false, authTask.exception?.message ?: "Error en registro.")
                }
            }
    }

    // --- GESTIÓN DE USUARIOS (Firestore) ---

    private fun saveUserInfoToFirestore(
        uid: String,
        name: String,
        email: String,
        additionalData: Map<String, Any>,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        val userInfo = hashMapOf<String, Any>(
            "uid" to uid,
            "displayName" to name,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )
        userInfo.putAll(additionalData)

        // Guardamos en la colección "usuarios"
        db.collection("usuarios").document(uid)
            .set(userInfo)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

    fun getUserRole(uid: String, onResult: (role: String?) -> Unit) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onResult(document.getString("role"))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    // --- GESTIÓN DE ALUMNOS ---

    fun getStudentsBySchool(schoolName: String, onResult: (List<Student>) -> Unit) {
        db.collection("usuarios")
            .whereEqualTo("school", schoolName)
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { documents ->
                // Convierte los documentos a objetos Student
                val studentList = documents.toObjects(Student::class.java)
                onResult(studentList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // --- GESTIÓN DE PICTOGRAMAS ---

    // Solo guardamos los datos (la URL ya viene como texto)
    fun savePictogram(pictogramData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        db.collection("pictogramas")
            .add(pictogramData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}