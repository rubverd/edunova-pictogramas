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

    // Obtener el Centro (School) del Profesor
    fun getTeacherSchool(uid: String, onResult: (String?) -> Unit) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                onResult(document.getString("school"))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun getUserData(uid: String, onResult: (Map<String, Any>?) -> Unit) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onResult(document.data)
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
                val studentList = documents.toObjects(Student::class.java)
                onResult(studentList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun getStudentAttempts(studentUid: String, onResult: (List<StudentAttempt>) -> Unit) {
        db.collection("intentos_alumnos")
            .whereEqualTo("studentUid", studentUid)
            .get()
            .addOnSuccessListener { documents ->
                val attempts = documents.map { doc ->
                    doc.toObject(StudentAttempt::class.java).copy(id = doc.id)
                }
                onResult(attempts.sortedByDescending { it.timestamp })
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onResult(emptyList())
            }
    }

    fun saveStudentAttempt(attemptData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        db.collection("intentos_alumnos")
            .add(attemptData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // --- GESTIÓN DE PICTOGRAMAS ---

    fun savePictogram(pictogramData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        db.collection("palabras")
            .add(pictogramData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // --- GESTIÓN DE FRASES (NUEVO) ---

    /**
     * Guarda una nueva frase en la colección "frases".
     * Separa automáticamente la frase en palabras individuales para facilitar el juego.
     */
    fun savePhrase(frase: String, imageUrl: String, dificultad: Int, onComplete: (Boolean) -> Unit) {

        // 1. Limpiamos espacios extra al inicio/final y separamos por espacios en blanco.
        // El regex "\\s+" asegura que si hay 2 espacios seguidos, no cree una palabra vacía.
        val palabrasSeparadas = frase.trim().split("\\s+".toRegex())

        // 2. Preparamos el objeto para Firebase
        val phraseData = hashMapOf(
            "frase" to frase.trim(),
            "urlImagen" to imageUrl,
            "dificultad" to dificultad, // Viene del Slider (1, 2 o 3)
            "palabras" to palabrasSeparadas, // Array ["El", "rio", "corre"]
            "createdAt" to System.currentTimeMillis()
        )

        // 3. Guardamos en la colección 'frases'
        db.collection("frases")
            .add(phraseData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onComplete(false)
            }
    }
}