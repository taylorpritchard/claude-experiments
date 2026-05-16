package com.stormquest.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.stormquest.R
import com.stormquest.data.Enemies
import com.stormquest.model.BattleEnemy
import com.stormquest.model.GameState
import kotlin.random.Random

class ExploreActivity : AppCompatActivity() {

    private lateinit var tvFloor: TextView
    private lateinit var tvGold: TextView
    private lateinit var tvLog: TextView
    private lateinit var llPartyStatus: LinearLayout
    private lateinit var btnAdvance: Button
    private lateinit var btnRest: Button
    private lateinit var btnShop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore)

        tvFloor = findViewById(R.id.tvFloor)
        tvGold = findViewById(R.id.tvGold)
        tvLog = findViewById(R.id.tvLog)
        llPartyStatus = findViewById(R.id.llPartyStatus)
        btnAdvance = findViewById(R.id.btnAdvance)
        btnRest = findViewById(R.id.btnRest)
        btnShop = findViewById(R.id.btnShop)

        btnAdvance.setOnClickListener { SoundManager.click(); onAdvance() }
        btnRest.setOnClickListener    { SoundManager.click(); onRest() }
        btnShop.setOnClickListener    { SoundManager.click(); onShop() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        SaveManager.save(this)
    }

    private fun refresh() {
        tvFloor.text = "Floor ${GameState.currentFloor}"
        tvGold.text = "Gold: ${GameState.gold}"
        updateLog()
        updatePartyStatus()

        if (!GameState.isPartyAlive()) {
            showGameOver()
        }
    }

    private fun updateLog() {
        val logs = GameState.getLastLogs(5)
        tvLog.text = logs.joinToString("\n")
    }

    private fun updatePartyStatus() {
        llPartyStatus.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (char in GameState.party) {
            val row = inflater.inflate(R.layout.item_party_status_row, llPartyStatus, false)
            val tvName = row.findViewById<TextView>(R.id.tvCharName)
            val tvClass = row.findViewById<TextView>(R.id.tvCharClass)
            val tvHpVal = row.findViewById<TextView>(R.id.tvHpValue)
            val tvMpVal = row.findViewById<TextView>(R.id.tvMpValue)
            val pbHp = row.findViewById<ProgressBar>(R.id.pbHp)
            val pbMp = row.findViewById<ProgressBar>(R.id.pbMp)

            tvName.text = char.name
            tvClass.text = "Lv${char.level} ${char.characterClass.displayName}"
            tvHpVal.text = "${char.currentHp}/${char.maxHp}"
            tvMpVal.text = "${char.currentMp}/${char.maxMp}"
            pbHp.max = char.maxHp
            pbHp.progress = char.currentHp
            pbMp.max = if (char.maxMp > 0) char.maxMp else 1
            pbMp.progress = char.currentMp

            if (!char.isAlive) {
                tvName.setTextColor(Color.parseColor("#555555"))
                tvClass.setTextColor(Color.parseColor("#555555"))
                tvHpVal.setTextColor(Color.parseColor("#555555"))
            }

            llPartyStatus.addView(row)
        }
    }

    private fun onAdvance() {
        if (!GameState.isPartyAlive()) {
            showGameOver()
            return
        }

        GameState.advanceFloor()
        GameState.addLog("--- Entering Floor ${GameState.currentFloor} ---")

        if (GameState.isBossFloor()) {
            // Boss battle
            GameState.addLog("A powerful presence fills the air...")
            val bossEnemy = BattleEnemy.fromData(Enemies.SHADOW_DRAGON)
            startBattle(listOf(bossEnemy))
        } else {
            // Random encounter chance: 70%
            val roll = Random.nextInt(100)
            if (roll < 70) {
                val enemies = generateRandomEncounter()
                GameState.addLog("Enemies appear! ${enemies.map { it.displayName }.joinToString(", ")}")
                startBattle(enemies)
            } else {
                GameState.addLog("You advance safely...")
                refresh()
            }
        }
    }

    private fun generateRandomEncounter(): List<BattleEnemy> {
        val pool = Enemies.getEnemiesForFloor(GameState.currentFloor)
        val count = (1..4).random()
        val enemyData = pool.random()
        return (0 until count).map { BattleEnemy.fromData(enemyData, if (count > 1) it else 0) }
    }

    private fun startBattle(enemies: List<BattleEnemy>) {
        val intent = Intent(this, BattleActivity::class.java)
        intent.putExtra(BattleActivity.EXTRA_ENEMIES, ArrayList(enemies))
        startActivity(intent)
    }

    private fun onRest() {
        if (GameState.gold < 10) {
            Toast.makeText(this, "Not enough gold to rest! (Need 10)", Toast.LENGTH_SHORT).show()
            return
        }
        GameState.gold -= 10
        for (char in GameState.party) {
            if (char.isAlive) {
                char.currentHp = char.maxHp
                char.currentMp = char.maxMp
            }
        }
        GameState.addLog("The party rests and recovers (cost: 10 gold).")
        refresh()
    }

    private fun onShop() {
        val intent = Intent(this, ShopActivity::class.java)
        startActivity(intent)
    }

    private fun showGameOver() {
        AlertDialog.Builder(this)
            .setTitle("GAME OVER")
            .setMessage("Your party has been defeated. The darkness claims you...")
            .setCancelable(false)
            .setPositiveButton("Return to Title") { _, _ ->
                val intent = Intent(this, com.stormquest.MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }
}
