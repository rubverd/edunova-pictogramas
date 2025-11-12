package com.example.edunova // Asegúrate de que este es tu paquete correcto


import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.edunova.databinding.SilabasBinding // ¡Importante! El nombre cambia a SilabasBinding

class SilabasActivity : AppCompatActivity() {

    // Declara la variable para ViewBinding, que se genera a partir de 'silabas.xml'.
    private lateinit var letterMap: Map<Char, TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Infla el layout 'silabas.xml' usando su clase de binding generada.



        fun cambiarColor(letra: Char, colorResId: Int) {
            // 1. Busca el TextView en el mapa usando el caracter
            val textView = letterMap[letra]
            // 2. Si se encuentra, cambia su color
            textView?.setTextColor(ContextCompat.getColor(this, colorResId))
        }




        // Aquí puedes añadir la lógica para cuando el usuario pulse una letra.
        // Por ejemplo, para la letra 'B':


        // Para la letra 'C':



    }
    // Función auxiliar para inicializar el mapa

}
