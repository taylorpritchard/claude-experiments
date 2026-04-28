package com.stormquest.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.stormquest.R
import com.stormquest.data.CharacterClass
import com.stormquest.data.CharacterClasses
import com.stormquest.data.Race
import com.stormquest.data.Races
import com.stormquest.model.GameCharacter
import com.stormquest.model.GameState

class PartyCreationActivity : AppCompatActivity() {

    private val PARTY_SIZE = 4
    private var currentCharIndex = 0
    private var selectedRace: Race = Races.HUMAN
    private var selectedClass: CharacterClass = CharacterClasses.WARRIOR

    // UI references
    private lateinit var tvProgress: TextView
    private lateinit var etName: EditText
    private lateinit var llRaceCards: LinearLayout
    private lateinit var llClassCards: LinearLayout
    private lateinit var tvStatsPreview: TextView
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party_creation)

        tvProgress = findViewById(R.id.tvProgress)
        etName = findViewById(R.id.etCharName)
        llRaceCards = findViewById(R.id.llRaceCards)
        llClassCards = findViewById(R.id.llClassCards)
        tvStatsPreview = findViewById(R.id.tvStatsPreview)
        btnNext = findViewById(R.id.btnNext)

        buildRaceCards()
        buildClassCards()
        updateUI()

        btnNext.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val character = GameCharacter.create(name, selectedRace, selectedClass)
            GameState.party.add(character)

            currentCharIndex++
            if (currentCharIndex >= PARTY_SIZE) {
                // Start the game
                val intent = Intent(this, ExploreActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // Reset for next character
                etName.setText("")
                selectedRace = Races.HUMAN
                selectedClass = CharacterClasses.WARRIOR
                buildRaceCards()
                buildClassCards()
                updateUI()
            }
        }
    }

    private fun buildRaceCards() {
        llRaceCards.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (race in Races.ALL) {
            val card = inflater.inflate(R.layout.item_selection_card, llRaceCards, false)
            val tvName = card.findViewById<TextView>(R.id.tvCardName)
            val tvDesc = card.findViewById<TextView>(R.id.tvCardDesc)
            tvName.text = race.displayName
            tvDesc.text = race.description
            card.tag = race.id
            card.setOnClickListener {
                selectedRace = race
                highlightCards(llRaceCards, race.id)
                updateStatsPreview()
            }
            llRaceCards.addView(card)
        }
        highlightCards(llRaceCards, selectedRace.id)
    }

    private fun buildClassCards() {
        llClassCards.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (cls in CharacterClasses.ALL) {
            val card = inflater.inflate(R.layout.item_selection_card, llClassCards, false)
            val tvName = card.findViewById<TextView>(R.id.tvCardName)
            val tvDesc = card.findViewById<TextView>(R.id.tvCardDesc)
            tvName.text = cls.displayName
            tvDesc.text = cls.description
            card.tag = cls.id
            card.setOnClickListener {
                selectedClass = cls
                highlightCards(llClassCards, cls.id)
                updateStatsPreview()
            }
            llClassCards.addView(card)
        }
        highlightCards(llClassCards, selectedClass.id)
    }

    private fun highlightCards(container: LinearLayout, selectedId: String) {
        for (i in 0 until container.childCount) {
            val card = container.getChildAt(i)
            if (card.tag == selectedId) {
                card.setBackgroundResource(R.drawable.card_selected)
            } else {
                card.setBackgroundResource(R.drawable.card_normal)
            }
        }
    }

    private fun updateStatsPreview() {
        val maxHp = selectedClass.baseHp + selectedRace.hpBonus
        val maxMp = selectedClass.baseMp + selectedRace.mpBonus
        val str = selectedClass.baseStr + selectedRace.strBonus
        val def = selectedClass.baseDef + selectedRace.defBonus
        val mag = selectedClass.baseMag + selectedRace.magBonus
        val spd = selectedClass.baseSpd + selectedRace.spdBonus
        val lck = selectedClass.baseLck + selectedRace.lckBonus
        tvStatsPreview.text = """
            HP: $maxHp    MP: $maxMp
            STR: $str    DEF: $def    MAG: $mag
            SPD: $spd    LCK: $lck
        """.trimIndent()
    }

    private fun updateUI() {
        tvProgress.text = "Character ${currentCharIndex + 1} of $PARTY_SIZE"
        btnNext.text = if (currentCharIndex == PARTY_SIZE - 1) "Begin Quest!" else "Next >"
        updateStatsPreview()
    }
}
