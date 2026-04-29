package com.stormquest.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private lateinit var llProgressDots: LinearLayout
    private lateinit var etName: EditText
    private lateinit var llRaceCards: LinearLayout
    private lateinit var llClassCards: LinearLayout
    private lateinit var llStatsPreview: LinearLayout
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party_creation)

        llProgressDots = findViewById(R.id.llProgressDots)
        etName = findViewById(R.id.etCharName)
        llRaceCards = findViewById(R.id.llRaceCards)
        llClassCards = findViewById(R.id.llClassCards)
        llStatsPreview = findViewById(R.id.llStatsPreview)
        btnNext = findViewById(R.id.btnNext)

        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { refreshNextButton() }
        })

        buildRaceCards()
        buildClassCards()
        updateUI()

        btnNext.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SoundManager.nextChar()
            val character = GameCharacter.create(name, selectedRace, selectedClass)
            GameState.party.add(character)

            currentCharIndex++
            if (currentCharIndex >= PARTY_SIZE) {
                val intent = Intent(this, ExploreActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                etName.setText("")
                selectedRace = Races.HUMAN
                selectedClass = CharacterClasses.WARRIOR
                buildRaceCards()
                buildClassCards()
                updateUI()
            }
        }
    }

    private fun refreshNextButton() {
        val enabled = etName.text.toString().trim().isNotEmpty()
        btnNext.isEnabled = enabled
        btnNext.alpha = if (enabled) 1f else 0.45f
    }

    private fun buildProgressDots() {
        llProgressDots.removeAllViews()
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        val dotSize = (10 * resources.displayMetrics.density).toInt()

        for (i in 0 until PARTY_SIZE) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(dotSize, dotSize)
            params.marginEnd = dp4
            params.marginStart = dp4
            dot.layoutParams = params

            dot.background = ContextCompat.getDrawable(this, R.drawable.dot_shape)?.mutate()

            val color = when {
                i < currentCharIndex -> Color.parseColor("#8B6914")  // bronze = done
                i == currentCharIndex -> Color.parseColor("#FFD700")  // gold = current
                else -> Color.parseColor("#3A3A4A")                  // dim = future
            }
            dot.background?.setTint(color)

            llProgressDots.addView(dot)
        }
    }

    private fun buildRaceCards() {
        llRaceCards.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (race in Races.ALL) {
            val card = inflater.inflate(R.layout.item_selection_card, llRaceCards, false)
            val tvName = card.findViewById<TextView>(R.id.tvCardName)
            val tvDesc = card.findViewById<TextView>(R.id.tvCardDesc)
            val ivPortrait = card.findViewById<ImageView>(R.id.ivPortrait)
            tvName.text = race.displayName
            tvDesc.text = race.description
            val portrait = CharacterArt.getRace(this, race.id)
            if (portrait != null) ivPortrait.setImageBitmap(portrait)
            else ivPortrait.visibility = View.GONE
            card.tag = race.id
            card.setOnClickListener {
                SoundManager.select()
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
            val ivPortrait = card.findViewById<ImageView>(R.id.ivPortrait)
            tvName.text = cls.displayName
            tvDesc.text = cls.description
            val portrait = CharacterArt.getCharClass(this, cls.id)
            if (portrait != null) ivPortrait.setImageBitmap(portrait)
            else ivPortrait.visibility = View.GONE
            card.tag = cls.id
            card.setOnClickListener {
                SoundManager.select()
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
                card.animate()
                    .scaleX(1.08f).scaleY(1.08f)
                    .setDuration(90)
                    .withEndAction {
                        card.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                    }.start()
            } else {
                card.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                card.setBackgroundResource(R.drawable.card_normal)
            }
        }
    }

    private fun updateStatsPreview() {
        statBarIndex = 0
        llStatsPreview.removeAllViews()
        val maxHp  = selectedClass.baseHp  + selectedRace.hpBonus
        val maxMp  = selectedClass.baseMp  + selectedRace.mpBonus
        val str    = selectedClass.baseStr + selectedRace.strBonus
        val def    = selectedClass.baseDef + selectedRace.defBonus
        val mag    = selectedClass.baseMag + selectedRace.magBonus
        val spd    = selectedClass.baseSpd + selectedRace.spdBonus
        val lck    = selectedClass.baseLck + selectedRace.lckBonus

        addStatBar("HP",  maxHp, 70,  "#e05050")
        addStatBar("MP",  maxMp, 65,  "#5070e0")
        addStatBar("STR", str,   15,  "#e07830")
        addStatBar("DEF", def,   15,  "#50a060")
        addStatBar("MAG", mag,   15,  "#9050e0")
        addStatBar("SPD", spd,   14,  "#50c8c0")
        addStatBar("LCK", lck,   12,  "#e0c050")
    }

    private var statBarIndex = 0

    private fun addStatBar(label: String, value: Int, max: Int, hexColor: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val dp2 = (2 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp2 * 2
            layoutParams = params
        }

        val dp = resources.displayMetrics.density

        val tvLabel = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@PartyCreationActivity, R.color.text_secondary))
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val barIdx = statBarIndex++
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            this.max = max
            progress = 0
            val params = LinearLayout.LayoutParams(0, (8 * dp).toInt(), 1f)
            params.gravity = android.view.Gravity.CENTER_VERTICAL
            params.marginStart = (6 * dp).toInt()
            params.marginEnd = (6 * dp).toInt()
            layoutParams = params
            progressDrawable?.setTint(Color.parseColor(hexColor))
        }

        val tvValue = TextView(this).apply {
            text = value.toString()
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@PartyCreationActivity, R.color.text_primary))
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        row.addView(tvLabel)
        row.addView(bar)
        row.addView(tvValue)
        llStatsPreview.addView(row)

        ObjectAnimator.ofInt(bar, "progress", 0, value).apply {
            duration = 350
            startDelay = barIdx * 40L
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun updateUI() {
        buildProgressDots()
        btnNext.text = if (currentCharIndex == PARTY_SIZE - 1) "Begin Quest!" else "Next >"
        refreshNextButton()
        updateStatsPreview()
    }
}
