package com.example.edunova


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.edunova.databinding.ItemWordBinding // Asegúrate de que este import es correcto

class WordAdapter(private val words: List<String>) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    /**
     * El ViewHolder contiene la referencia a las vistas de una sola fila (en este caso, solo un TextView).
     * Usa ViewBinding para acceder a ellas de forma segura.
     */
    inner class WordViewHolder(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Se llama cuando el RecyclerView necesita crear una nueva fila.
     * Aquí "inflamos" (creamos) el layout 'item_word.xml' y lo pasamos al ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding)
    }

    /**
     * Se llama para mostrar los datos en una fila específica.
     * Aquí tomamos la palabra de la lista y la asignamos al TextView de la fila.
     */
    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]
        holder.binding.textViewWord.text = word
    }

    /**
     * Devuelve el número total de elementos en la lista.
     * El RecyclerView lo usa para saber cuántas filas debe dibujar.
     */
    override fun getItemCount(): Int {
        return words.size
    }
}
