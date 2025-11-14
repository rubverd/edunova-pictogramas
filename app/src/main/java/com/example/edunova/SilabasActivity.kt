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


            val silabas = listOf(
                "ba", "be", "bi", "bo", "bu", "ca", "ce", "ci", "co", "cu",
                "da", "de", "di", "do", "du", "fa", "fe", "fi", "fo", "fu",
                "ga", "ge", "gi", "go", "gu", "gui", "gue", "ha", "he", "hi", "ho", "hu",
                "ja", "je", "ji", "jo", "ju", "ka", "ke", "ki", "ko", "ku",
                "la", "le", "li", "lo", "lu", "ma", "me", "mi", "mo", "mu",
                "na", "ne", "ni", "no", "nu", "ña", "ñe", "ñi", "ño", "ñu", "pa", "pe", "pi", "po", "pu",
                "que", "qui", "ra", "re", "ri", "ro", "ru", "sa", "se", "si", "so", "su",
                "ta", "te", "ti", "to", "tu", "va", "ve", "vi", "vo", "vu",
                "wa", "we", "wi", "wo", "wu", "xa", "xe", "xi", "xo", "xu",
                "ya", "ye", "yi", "yo", "yu", "za", "ze", "zi", "zo", "zu"
            )

            // Agrupar las sílabas por su letra inicial
            val silabasAgrupadas: Map<Char, List<String>> = silabas.groupBy { it.first() }

            // --- Ejemplo de cómo acceder a las listas ---
            val silabasConB = silabasAgrupadas['b'] // -> [ba, be, bi, bo, bu]
            val silabasConC = silabasAgrupadas['c'] // -> [ca, ce, ci, co, cu]
            val silabasConD = silabasAgrupadas['d'] // -> [da, de, di, do, du]
            val silabasConF = silabasAgrupadas['f'] // -> [fa, fe, fi, fo, fu]
            val silabasConG = silabasAgrupadas['g'] // -> [ga, ge, gi, go, gu, gui, gue]
            val silabasConH = silabasAgrupadas['h'] // -> [ha, he, hi, ho, hu]
            val silabasConJ = silabasAgrupadas['j'] // -> [ja, je, ji, jo, ju]
            val silabasConK = silabasAgrupadas['k'] // -> [ka, ke, ki, ko, ku]
            val silabasConL = silabasAgrupadas['l'] // -> [la, le, li, lo, lu]
            val silabasConM = silabasAgrupadas['m'] // -> [ma, me, mi, mo, mu]
            val silabasConN = silabasAgrupadas['n'] // -> [na, ne, ni, no, nu]
            val silabasConNN = silabasAgrupadas['ñ']
            val silabasConP = silabasAgrupadas['p'] // -> [pa, pe, pi, po, pu]
            val silabasConQ = silabasAgrupadas['q'] // -> [que, qui]
            val silabasConR = silabasAgrupadas['r'] // -> [ra, re, ri, ro, ru]
            val silabasConS = silabasAgrupadas['s'] // -> [sa, se, si, so, su]
            val silabasConT = silabasAgrupadas['t'] // -> [ta, te, ti, to, tu]
            val silabasConV = silabasAgrupadas['v'] // -> [va, ve, vi, vo, vu]
            val silabasConW = silabasAgrupadas['w'] // -> [wa, we, wi, wo, wu]
            val silabasConX = silabasAgrupadas['x'] // -> [xa, xe, xi, xo, xu]
            val silabasConY = silabasAgrupadas['y'] // -> [ya, ye, yi, yo, yu]
            val silabasConZ = silabasAgrupadas['z'] // -> [za, ze, zi, zo, zu]


        // Puedes iterar sobre todo el mapa para ver todos los grupos
            println("\n--- Todos los grupos ---")
            silabasAgrupadas.forEach { (letra, lista) ->
                println("Letra '$letra': $lista")
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
