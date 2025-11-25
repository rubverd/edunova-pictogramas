package com.example.edunova.db

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.edunova.clases.Student
import com.example.edunova.clases.StudentAttempt


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

    // --- GESTIÓN DE USUARIOS ---

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

    // --- NUEVO: Obtener el Centro (School) del Profesor ---
    fun getTeacherSchool(uid: String, onResult: (String?) -> Unit) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                // Devuelve el campo "school" o null si no existe
                onResult(document.getString("school"))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // --- GESTIÓN DE ALUMNOS ---

    fun getStudentsBySchool(schoolName: String, onResult: (List<Student>) -> Unit) {
        db.collection("usuarios")
            .whereEqualTo("school", schoolName)
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { documents ->
                val studentList = documents.toObjects(Student::class.java)
                onResult(studentList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // --- GESTIÓN DE PICTOGRAMAS ---

    fun savePictogram(pictogramData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        // CAMBIO IMPORTANTE: "palabras" para coincidir con tu base de datos real
        db.collection("palabras")
            .add(pictogramData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
    fun getStudentAttempts(studentUid: String, onResult: (List<StudentAttempt>) -> Unit) {
        db.collection("intentos_alumnos")
            .whereEqualTo("studentUid", studentUid)
            // .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Habilita esto cuando crees el índice en Firebase
            .get()
            .addOnSuccessListener { documents ->
                val attempts = documents.map { doc ->
                    doc.toObject(StudentAttempt::class.java).copy(id = doc.id)
                }
                // Ordenamos por fecha en código si el índice de Firebase falla al principio
                onResult(attempts.sortedByDescending { it.timestamp })
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onResult(emptyList())
            }
    }
}