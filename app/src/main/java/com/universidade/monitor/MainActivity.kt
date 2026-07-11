package com.universidade.monitor

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textHello = findViewById<TextView>(R.id.textHello)
        val buttonStart = findViewById<Button>(R.id.buttonStart)

        buttonStart.setOnClickListener {
            textHello.text = "App recomeçado com sucesso"
            Toast.makeText(this, "Projeto do zero pronto", Toast.LENGTH_SHORT).show()
        }
    }
}