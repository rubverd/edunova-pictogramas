package com.example.edunova // Asegúrate de que este es tu paquete correcto


import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.edunova.databinding.SilabasBinding // ¡Importante! El nombre cambia a SilabasBinding

class SilabasActivity : AppCompatActivity() {

    // Declara la variable para ViewBinding, que se genera a partir de 'silabas.xml'.
    private lateinit var binding: SilabasBinding
    private lateinit var letterMap: Map<Char, TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Infla el layout 'silabas.xml' usando su clase de binding generada.
        binding = SilabasBinding.inflate(layoutInflater)

        // 2. Establece la vista de la actividad.
        setContentView(binding.root)

        // 3. Configura el OnClickListener para el botón de retorno.
        binding.returnButton.setOnClickListener {
            // Cierra la actividad actual (SilabasActivity) y regresa a la pantalla anterior.
            finish()
        }



        fun cambiarColor(letra: Char, colorResId: Int) {
            // 1. Busca el TextView en el mapa usando el caracter
            val textView = letterMap[letra]
            // 2. Si se encuentra, cambia su color
            textView?.setTextColor(ContextCompat.getColor(this, colorResId))
        }




        // Aquí puedes añadir la lógica para cuando el usuario pulse una letra.
        // Por ejemplo, para la letra 'B':
        binding.B.setOnClickListener {
            // Actualiza el TextView grande o realiza otra acción.
            binding.bigTextView.text = "B"
        }

        // Para la letra 'C':
        binding.C.setOnClickListener {
            binding.bigTextView.text = "C"
        }


    }
    // Función auxiliar para inicializar el mapa
    private fun initializeLetterMap() {
        letterMap = mapOf(
            'B' to binding.B,
            'C' to binding.C,
            'D' to binding.D,
            'F' to binding.F,
            'G' to binding.G,
            'H' to binding.H,
            'J' to binding.J,
            'K' to binding.K,
            'L' to binding.L,
            'M' to binding.M,
            'N' to binding.N,
            'Ñ' to binding.NEne,
            'P' to binding.P,
            'Q' to binding.Q,
            'R' to binding.letraR,
            'S' to binding.S,
            'T' to binding.T,
            'V' to binding.V,
            'W' to binding.W,
            'X' to binding.X,
            'Y' to binding.Y,
            'Z' to binding.Z
            // Añade el resto de TextViews aquí
        )
    }
}
