package com.example.edunova; // Asegúrate de que este sea tu paquete correcto

import android.content.Intent;
import android.os.Bundle;import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.edunova.JuegoPalabras;
import com.example.edunova.R;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Establece la vista del layout menu.xml para esta actividad
        setContentView(R.layout.menu);

        // Obtiene la referencia del botón con el id button7
        Button botonJuegoPalabras = findViewById(R.id.button7);

        // Configura un listener para el evento de clic en el botón
        botonJuegoPalabras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Crea un Intent para iniciar JuegoPalabrasActivity
                Intent intent = new Intent(MenuActivity.this, JuegoPalabras.class);
                startActivity(intent);
            }
        });
    }
}
