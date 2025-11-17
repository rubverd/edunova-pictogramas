package com.example.edunova.db

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseConnection {

    // Instancias de Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getFirestoreInstance(): FirebaseFirestore {
        return db
    }

    /**
     * Registra un usuario y guarda datos adicionales.
     */
    fun registerUser(
        name: String,
        email: String,
        pass: String,
        additionalData: Map<String, Any>, // Recibe el mapa genérico
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Llamamos a guardar info pasando el mapa correctamente
                        saveUserInfoToFirestore(firebaseUser.uid, name, email, additionalData, onComplete)
                    } else {
                        onComplete(false, "No se pudo obtener el UID del usuario.")
                    }
                } else {
                    onComplete(false, authTask.exception?.message ?: "Error en el registro.")
                }
            }
    }

    /**
     * Login de usuario
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

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // --- MÉTODOS DE FIRESTORE ---

    /**
     * Guarda la información. Corregido el tipo de HashMap.
     */
    fun getUserRole(uid: String, onResult: (role: String?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    onResult(role)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // Método privado para guardar info
    private fun saveUserInfoToFirestore(
        uid: String,
        name: String,
        email: String,
        additionalData: Map<String, Any>,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        // CORRECCIÓN CLAVE: Especificamos <String, Any> explícitamente
        val userInfo = hashMapOf<String, Any>(
            "uid" to uid,
            "displayName" to name,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        // Ahora sí funciona el putAll porque los tipos coinciden
        userInfo.putAll(additionalData)

        // Guardamos en la colección "usuarios" (unificado con el resto de la app)
        db.collection("usuarios").document(uid)
            .set(userInfo)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, "Error al guardar datos: ${e.message}")
            }
    }

    // Obtener alumnos para un profesor
    fun getStudentsForTeacher(teacherUid: String, onResult: (List<Pair<String, String>>) -> Unit) {
        db.collection("users")
            .whereEqualTo("assignedTeacherId", teacherUid)
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { documents ->
                val studentList = mutableListOf<Pair<String, String>>()
                for (document in documents) {
                    val uid = document.id
                    val name = document.getString("displayName") ?: "Sin nombre"
                    studentList.add(Pair(uid, name))
                }
                onResult(studentList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}