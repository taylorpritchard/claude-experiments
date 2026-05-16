package com.stormquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.stormquest.model.GameState
import com.stormquest.ui.ExploreActivity
import com.stormquest.ui.PartyCreationActivity
import com.stormquest.ui.SaveManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnNewGame  = findViewById<Button>(R.id.btnNewGame)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val tvVersion   = findViewById<TextView>(R.id.tvVersion)

        tvVersion.text = "v1.0 — StormQuest"

        val hasSave = SaveManager.hasSave(this)
        btnContinue.isEnabled = hasSave
        btnContinue.alpha     = if (hasSave) 1f else 0.4f

        btnContinue.setOnClickListener {
            if (SaveManager.load(this)) {
                startActivity(Intent(this, ExploreActivity::class.java))
            }
        }

        btnNewGame.setOnClickListener {
            if (SaveManager.hasSave(this)) {
                AlertDialog.Builder(this)
                    .setTitle("New Game")
                    .setMessage("Start a new game? Your current save will be lost.")
                    .setPositiveButton("New Game") { _, _ -> startNewGame() }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                startNewGame()
            }
        }
    }

    private fun startNewGame() {
        SaveManager.deleteSave(this)
        GameState.initialize()
        GameState.party.clear()
        startActivity(Intent(this, PartyCreationActivity::class.java))
    }
}
