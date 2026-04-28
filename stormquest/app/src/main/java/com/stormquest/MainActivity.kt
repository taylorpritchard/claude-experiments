package com.stormquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stormquest.model.GameState
import com.stormquest.ui.PartyCreationActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnNewGame = findViewById<Button>(R.id.btnNewGame)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val tvVersion = findViewById<TextView>(R.id.tvVersion)

        tvVersion.text = "v1.0 — StormQuest"

        btnContinue.isEnabled = false
        btnContinue.alpha = 0.4f

        btnNewGame.setOnClickListener {
            GameState.initialize()
            GameState.party.clear()
            val intent = Intent(this, PartyCreationActivity::class.java)
            startActivity(intent)
        }
    }
}
