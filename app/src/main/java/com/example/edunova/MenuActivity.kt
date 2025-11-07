package com.example.edunova // Asegúrate de que este sea tu paquete correcto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establece la vista del layout menu.xml para esta actividad
        setContentView(R.layout.menu)

        // Obtiene la referencia del botón con el id button7
        val botonJuegoPalabras = findViewById<Button?>(R.id.button7)

        // Configura un listener para el evento de clic en el botón
        botonJuegoPalabras.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // Crea un Intent para iniciar JuegoPalabrasActivity
                val intent = Intent(this@MenuActivity, JuegoPalabras::class.java)
                startActivity(intent)
            }
        })
    }
}