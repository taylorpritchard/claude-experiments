package com.stormquest.ui

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.stormquest.R
import com.stormquest.battle.ActionType
import com.stormquest.battle.BattleAction
import com.stormquest.battle.BattleEngine
import com.stormquest.data.Items
import com.stormquest.data.Spell
import com.stormquest.data.SpellTarget
import com.stormquest.data.ItemEffect
import com.stormquest.model.BattleEnemy
import com.stormquest.model.GameCharacter
import com.stormquest.model.GameState

class BattleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENEMIES = "extra_enemies"
    }

    // State
    private lateinit var enemies: MutableList<BattleEnemy>
    private val party: List<GameCharacter> get() = GameState.party
    private lateinit var engine: BattleEngine
    private val battleLog = mutableListOf<String>()
    private val playerActions = mutableMapOf<GameCharacter, BattleAction>()
    private var currentActorIndex = 0
    private var battleOver = false

    // Spell selection state
    private var pendingSpell: Spell? = null

    // UI references
    private lateinit var tvBattleHeader: TextView
    private lateinit var llEnemyStatus: LinearLayout
    private lateinit var tvBattleLog: TextView
    private lateinit var llPartyBattle: LinearLayout
    private lateinit var tvTurnIndicator: TextView

    // Action panels
    private lateinit var llMainActions: LinearLayout
    private lateinit var llEnemyTargets: LinearLayout
    private lateinit var llSpellList: LinearLayout
    private lateinit var llAllyTargets: LinearLayout
    private lateinit var llItemList: LinearLayout

    // Main action buttons
    private lateinit var btnAttack: Button
    private lateinit var btnMagic: Button
    private lateinit var btnItem: Button
    private lateinit var btnDefend: Button
    private lateinit var btnFlee: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        @Suppress("UNCHECKED_CAST")
        val enemyList = intent.getSerializableExtra(EXTRA_ENEMIES) as? ArrayList<BattleEnemy>
            ?: arrayListOf()
        enemies = enemyList.toMutableList()
        engine = BattleEngine(party, enemies)

        bindViews()
        setupMainActionButtons()
        refresh()
        promptNextAction()
    }

    private fun bindViews() {
        tvBattleHeader = findViewById(R.id.tvBattleHeader)
        llEnemyStatus = findViewById(R.id.llEnemyStatus)
        tvBattleLog = findViewById(R.id.tvBattleLog)
        llPartyBattle = findViewById(R.id.llPartyBattle)
        tvTurnIndicator = findViewById(R.id.tvTurnIndicator)

        llMainActions = findViewById(R.id.llMainActions)
        llEnemyTargets = findViewById(R.id.llEnemyTargets)
        llSpellList = findViewById(R.id.llSpellList)
        llAllyTargets = findViewById(R.id.llAllyTargets)
        llItemList = findViewById(R.id.llItemList)

        btnAttack = findViewById(R.id.btnAttack)
        btnMagic = findViewById(R.id.btnMagic)
        btnItem = findViewById(R.id.btnItem)
        btnDefend = findViewById(R.id.btnDefend)
        btnFlee = findViewById(R.id.btnFlee)
    }

    private fun setupMainActionButtons() {
        btnAttack.setOnClickListener { SoundManager.click(); onAttackPressed() }
        btnMagic.setOnClickListener  { SoundManager.click(); showSpellList() }
        btnItem.setOnClickListener   { SoundManager.click(); showItemList() }
        btnDefend.setOnClickListener { SoundManager.click(); selectDefend() }
        btnFlee.setOnClickListener   { SoundManager.click(); confirmFlee() }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, -14f, 14f, -10f, 10f, -5f, 5f, 0f)
            .apply { duration = 280; start() }
    }

    private fun refresh() {
        // Enemy header
        val enemyNames = enemies.filter { it.isAlive }.joinToString(", ") { it.displayName }
        tvBattleHeader.text = if (enemyNames.isEmpty()) "VICTORY" else "vs $enemyNames"

        // Enemy status area
        llEnemyStatus.removeAllViews()
        for (enemy in enemies) {
            val nameRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(4, 4, 4, 2)
            }
            val tvName = TextView(this).apply {
                text = if (enemy.isAlive) enemy.displayName else "[Defeated] ${enemy.displayName}"
                setTextColor(if (enemy.isAlive) Color.parseColor("#e8e8e8") else Color.parseColor("#555555"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            }
            val tvHp = TextView(this).apply {
                text = if (enemy.isAlive) "HP: ${enemy.currentHp}/${enemy.data.maxHp}" else "---"
                setTextColor(Color.parseColor("#cc3333"))
                textSize = 12f
            }
            nameRow.addView(tvName)
            nameRow.addView(tvHp)

            val pbHp = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = enemy.data.maxHp
                progress = enemy.currentHp
                progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#cc3333"))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 12)
                lp.setMargins(4, 0, 4, 4)
                layoutParams = lp
                visibility = if (enemy.isAlive) View.VISIBLE else View.GONE
            }

            llEnemyStatus.addView(nameRow)
            llEnemyStatus.addView(pbHp)
        }

        // Party battle status
        llPartyBattle.removeAllViews()
        val aliveForAction = getAlivePartyMembers()
        for (char in party) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(6, 4, 6, 4)
            }
            val isCurrentActor = aliveForAction.getOrNull(currentActorIndex) == char
            val bgColor = when {
                !char.isAlive -> Color.parseColor("#1a1a1a")
                isCurrentActor -> Color.parseColor("#162016")
                else -> Color.parseColor("#0d0d1a")
            }
            row.setBackgroundColor(bgColor)

            val tvName = TextView(this).apply {
                text = char.name
                setTextColor(if (char.isAlive) Color.parseColor("#ffd700") else Color.parseColor("#555555"))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            }
            val tvClass = TextView(this).apply {
                text = "Lv${char.level}"
                setTextColor(Color.parseColor("#aaaaaa"))
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val hpFrac = char.currentHp.toFloat() / char.maxHp
            val tvHp = TextView(this).apply {
                text = "HP:${char.currentHp}/${char.maxHp}"
                setTextColor(when {
                    !char.isAlive -> Color.parseColor("#555555")
                    hpFrac < 0.25f -> Color.parseColor("#ff4444")
                    hpFrac < 0.5f -> Color.parseColor("#ffaa00")
                    else -> Color.parseColor("#44bb44")
                })
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            }
            val tvMp = TextView(this).apply {
                text = "MP:${char.currentMp}/${char.maxMp}"
                setTextColor(Color.parseColor("#4488ff"))
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            }
            val tvStatus = TextView(this).apply {
                text = when {
                    !char.isAlive -> "KO"
                    playerActions.containsKey(char) -> "RDY"
                    isCurrentActor -> ">>>"
                    else -> ""
                }
                setTextColor(if (playerActions.containsKey(char)) Color.parseColor("#44bb44")
                else Color.parseColor("#ffd700"))
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvName)
            row.addView(tvClass)
            row.addView(tvHp)
            row.addView(tvMp)
            row.addView(tvStatus)
            llPartyBattle.addView(row)

            val divider = View(this).apply {
                setBackgroundColor(Color.parseColor("#1a1a2e"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            }
            llPartyBattle.addView(divider)
        }
    }

    private fun updateBattleLog() {
        val last4 = if (battleLog.size <= 4) battleLog.toList() else battleLog.takeLast(4)
        tvBattleLog.text = last4.joinToString("\n")
    }

    private fun addLog(msg: String) {
        battleLog.add(msg)
        if (battleLog.size > 30) battleLog.removeAt(0)
        updateBattleLog()
    }

    private fun getAlivePartyMembers(): List<GameCharacter> = party.filter { it.isAlive }

    private fun promptNextAction() {
        if (battleOver) return
        val alive = getAlivePartyMembers()
        if (currentActorIndex >= alive.size) {
            executeRound()
            return
        }
        val actor = alive[currentActorIndex]
        tvTurnIndicator.text = "${actor.name}'s Turn"
        showMainActions()
        refresh()
    }

    // ========== PANEL VISIBILITY ==========

    private fun showMainActions() {
        llMainActions.visibility = View.VISIBLE
        llEnemyTargets.visibility = View.GONE
        llSpellList.visibility = View.GONE
        llAllyTargets.visibility = View.GONE
        llItemList.visibility = View.GONE
        pendingSpell = null

        val actor = getAlivePartyMembers().getOrNull(currentActorIndex)
        if (actor != null) {
            val hasSpells = actor.getAvailableSpells().isNotEmpty()
            btnMagic.isEnabled = hasSpells
            btnMagic.alpha = if (hasSpells) 1f else 0.4f
            val hasItems = GameState.inventory.isNotEmpty()
            btnItem.isEnabled = hasItems
            btnItem.alpha = if (hasItems) 1f else 0.4f
        }
    }

    // ========== ATTACK ==========

    private fun onAttackPressed() {
        val aliveEnemies = enemies.filter { it.isAlive }
        if (aliveEnemies.size == 1) {
            val actor = getAlivePartyMembers().getOrNull(currentActorIndex) ?: return
            recordAction(actor, BattleAction(ActionType.ATTACK, actor, targetEnemy = aliveEnemies[0]))
        } else {
            showEnemyTargetsForAttack()
        }
    }

    private fun showEnemyTargetsForAttack() {
        llMainActions.visibility = View.GONE
        llEnemyTargets.visibility = View.VISIBLE
        llSpellList.visibility = View.GONE
        llAllyTargets.visibility = View.GONE
        llItemList.visibility = View.GONE

        populateEnemyTargets("Select Enemy to Attack:") { enemy ->
            val actor = getAlivePartyMembers().getOrNull(currentActorIndex) ?: return@populateEnemyTargets
            recordAction(actor, BattleAction(ActionType.ATTACK, actor, targetEnemy = enemy))
        }
        addBackButton(llEnemyTargets) { showMainActions() }
    }

    // ========== MAGIC ==========

    private fun showSpellList() {
        val actor = getAlivePartyMembers().getOrNull(currentActorIndex) ?: return
        val spells = actor.getAvailableSpells()
        if (spells.isEmpty()) {
            Toast.makeText(this, "No spells available!", Toast.LENGTH_SHORT).show()
            return
        }

        llMainActions.visibility = View.GONE
        llEnemyTargets.visibility = View.GONE
        llSpellList.visibility = View.VISIBLE
        llAllyTargets.visibility = View.GONE
        llItemList.visibility = View.GONE

        llSpellList.removeAllViews()
        addLabel(llSpellList, "Cast Spell (MP: ${actor.currentMp}/${actor.maxMp}):", "#4488ff")

        for (spell in spells) {
            val canCast = actor.currentMp >= spell.mpCost
            val btn = makeButton(
                "${spell.displayName}  [${spell.mpCost}MP]  —  ${spell.description}",
                R.drawable.btn_spell,
                canCast
            )
            btn.setOnClickListener {
                if (!canCast) { Toast.makeText(this, "Not enough MP!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                pendingSpell = spell
                handleSpellSelected(actor, spell)
            }
            llSpellList.addView(btn)
        }
        addBackButton(llSpellList) { showMainActions() }
    }

    private fun handleSpellSelected(actor: GameCharacter, spell: Spell) {
        when (spell.target) {
            SpellTarget.SINGLE_ENEMY -> {
                val alive = enemies.filter { it.isAlive }
                if (alive.size == 1) {
                    recordAction(actor, BattleAction(ActionType.MAGIC, actor, targetEnemy = alive[0], spellId = spell.id))
                } else {
                    showEnemyTargetsForSpell(actor, spell)
                }
            }
            SpellTarget.ALL_ENEMIES -> {
                recordAction(actor, BattleAction(ActionType.MAGIC, actor, spellId = spell.id))
            }
            SpellTarget.SINGLE_ALLY -> {
                showAllyTargetsForSpell(actor, spell)
            }
            SpellTarget.ALL_ALLIES -> {
                recordAction(actor, BattleAction(ActionType.MAGIC, actor, spellId = spell.id))
            }
        }
    }

    private fun showEnemyTargetsForSpell(actor: GameCharacter, spell: Spell) {
        llSpellList.visibility = View.GONE
        llEnemyTargets.visibility = View.VISIBLE

        populateEnemyTargets("Cast ${spell.displayName} on:") { enemy ->
            recordAction(actor, BattleAction(ActionType.MAGIC, actor, targetEnemy = enemy, spellId = spell.id))
        }
        addBackButton(llEnemyTargets) { showSpellList() }
    }

    private fun showAllyTargetsForSpell(actor: GameCharacter, spell: Spell) {
        llSpellList.visibility = View.GONE
        llAllyTargets.visibility = View.VISIBLE

        llAllyTargets.removeAllViews()
        addLabel(llAllyTargets, "Cast ${spell.displayName} on:", "#ffd700")

        for (ally in party) {
            val canTarget = ally.isAlive
            val btn = makeButton(
                "${ally.name}${if (!ally.isAlive) " [KO]" else " HP:${ally.currentHp}/${ally.maxHp}"}",
                R.drawable.btn_ally_target,
                canTarget
            )
            btn.setOnClickListener {
                if (!canTarget) return@setOnClickListener
                recordAction(actor, BattleAction(ActionType.MAGIC, actor, targetCharacter = ally, spellId = spell.id))
            }
            llAllyTargets.addView(btn)
        }
        addBackButton(llAllyTargets) { showSpellList() }
    }

    // ========== ITEMS ==========

    private fun showItemList() {
        val actor = getAlivePartyMembers().getOrNull(currentActorIndex) ?: return
        val inv = GameState.inventory.toMap() // snapshot

        if (inv.isEmpty()) {
            Toast.makeText(this, "No items!", Toast.LENGTH_SHORT).show()
            return
        }

        llMainActions.visibility = View.GONE
        llEnemyTargets.visibility = View.GONE
        llSpellList.visibility = View.GONE
        llAllyTargets.visibility = View.GONE
        llItemList.visibility = View.VISIBLE

        llItemList.removeAllViews()
        addLabel(llItemList, "Select Item:", "#c0a060")

        for ((itemId, qty) in inv) {
            val item = Items.ALL.firstOrNull { it.id == itemId } ?: continue
            val btn = makeButton("${item.displayName} x$qty — ${item.description}", R.drawable.btn_item, true)
            btn.setOnClickListener {
                // Consume item here (reserve it); if user somehow backs out at a later subpanel
                // the item is still used (simplest approach - same as original FF behaviour)
                if (!GameState.removeItem(itemId)) {
                    Toast.makeText(this, "Item unavailable!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dispatchItemAction(actor, item.effect, itemId)
            }
            llItemList.addView(btn)
        }
        addBackButton(llItemList) { showMainActions() }
    }

    private fun dispatchItemAction(actor: GameCharacter, effect: ItemEffect, itemId: String) {
        when (effect) {
            ItemEffect.HEAL_HP_ALL -> {
                // No target needed
                recordAction(actor, BattleAction(ActionType.ITEM, actor, itemId = itemId))
            }
            ItemEffect.DAMAGE_SINGLE -> {
                val aliveEnemies = enemies.filter { it.isAlive }
                if (aliveEnemies.size == 1) {
                    recordAction(actor, BattleAction(ActionType.ITEM, actor, targetEnemy = aliveEnemies[0], itemId = itemId))
                } else {
                    showEnemyTargetsForItem(actor, itemId)
                }
            }
            ItemEffect.HEAL_HP_SINGLE, ItemEffect.HEAL_MP_SINGLE -> {
                showAllyTargetsForItem(actor, itemId, deadAllowed = false)
            }
            ItemEffect.REVIVE_SINGLE -> {
                val deadAllies = party.filter { !it.isAlive }
                if (deadAllies.isEmpty()) {
                    Toast.makeText(this, "No KO'd allies to revive!", Toast.LENGTH_SHORT).show()
                    GameState.addItem(itemId) // refund
                    showItemList()
                } else {
                    showAllyTargetsForItem(actor, itemId, deadAllowed = true)
                }
            }
        }
    }

    private fun showEnemyTargetsForItem(actor: GameCharacter, itemId: String) {
        llItemList.visibility = View.GONE
        llEnemyTargets.visibility = View.VISIBLE

        populateEnemyTargets("Throw at which enemy?") { enemy ->
            recordAction(actor, BattleAction(ActionType.ITEM, actor, targetEnemy = enemy, itemId = itemId))
        }
        // Note: back not provided here to avoid item refund complexity; item already consumed
    }

    private fun showAllyTargetsForItem(actor: GameCharacter, itemId: String, deadAllowed: Boolean) {
        llItemList.visibility = View.GONE
        llEnemyTargets.visibility = View.GONE
        llSpellList.visibility = View.GONE
        llAllyTargets.visibility = View.VISIBLE

        llAllyTargets.removeAllViews()
        val item = Items.ALL.firstOrNull { it.id == itemId }
        addLabel(llAllyTargets, "Use ${item?.displayName ?: "item"} on:", "#ffd700")

        val targets = if (deadAllowed) party.filter { !it.isAlive } else party.filter { it.isAlive }
        for (ally in targets) {
            val btn = makeButton(
                "${ally.name} HP:${ally.currentHp}/${ally.maxHp}${if (!ally.isAlive) " [KO]" else ""}",
                R.drawable.btn_ally_target, true
            )
            btn.setOnClickListener {
                recordAction(actor, BattleAction(ActionType.ITEM, actor, targetCharacter = ally, itemId = itemId))
            }
            llAllyTargets.addView(btn)
        }
    }

    // ========== DEFEND / FLEE ==========

    private fun selectDefend() {
        val actor = getAlivePartyMembers().getOrNull(currentActorIndex) ?: return
        recordAction(actor, BattleAction(ActionType.DEFEND, actor))
    }

    private fun confirmFlee() {
        AlertDialog.Builder(this)
            .setTitle("Flee?")
            .setMessage("Attempt to escape from battle?")
            .setPositiveButton("Flee!") { _, _ ->
                // Mark all remaining party members as flee
                for (a in getAlivePartyMembers()) {
                    playerActions[a] = BattleAction(ActionType.FLEE, a)
                }
                currentActorIndex = getAlivePartyMembers().size
                executeRound()
            }
            .setNegativeButton("Stay", null)
            .show()
    }

    // ========== ACTION RECORDING / ROUND EXECUTION ==========

    private fun recordAction(actor: GameCharacter, action: BattleAction) {
        playerActions[actor] = action
        currentActorIndex++
        promptNextAction()
    }

    private fun executeRound() {
        if (battleOver) return
        setActionsEnabled(false)
        tvTurnIndicator.text = "Resolving..."
        showMainActions()

        val result = engine.executeRound(playerActions)
        for (msg in result.messages) {
            addLog(msg)
        }

        val hasAttack = result.messages.any { it.contains("attacks") }
        val hasMagic  = result.messages.any { it.contains("casts") }
        val enemyHit  = enemies.any { e -> result.messages.any { it.contains(e.displayName) && it.contains("takes") } }
        val partyHit  = party.any  { c -> result.messages.any { it.contains(c.name)          && it.contains("takes") } }

        when {
            hasMagic  -> SoundManager.magic()
            hasAttack -> SoundManager.attack()
        }
        if (enemyHit)  shakeView(llEnemyStatus)
        if (partyHit)  shakeView(llPartyBattle)

        refresh()

        if (result.fled) {
            if (result.fleeSuccess) {
                showFledDialog()
            } else {
                resetForNewRound()
            }
            return
        }

        if (result.victory) {
            battleOver = true
            val (exp, gold) = engine.calculateRewards()
            showVictoryDialog(exp, gold)
            return
        }

        if (result.defeat) {
            battleOver = true
            showDefeatDialog()
            return
        }

        resetForNewRound()
    }

    private fun resetForNewRound() {
        playerActions.clear()
        currentActorIndex = 0
        setActionsEnabled(true)
        promptNextAction()
    }

    private fun setActionsEnabled(enabled: Boolean) {
        btnAttack.isEnabled = enabled
        btnMagic.isEnabled = enabled
        btnItem.isEnabled = enabled
        btnDefend.isEnabled = enabled
        btnFlee.isEnabled = enabled
    }

    // ========== OUTCOME DIALOGS ==========

    private fun showVictoryDialog(expGained: Int, goldGained: Int) {
        battleOver = true
        SoundManager.victory()
        tvTurnIndicator.text = "VICTORY!"
        tvTurnIndicator.setTextColor(Color.parseColor("#ffd700"))
        llMainActions.visibility = View.GONE

        GameState.gold += goldGained

        val levelUpMessages = mutableListOf<String>()
        for (char in party.filter { it.isAlive }) {
            val msgs = char.addExp(expGained)
            levelUpMessages.addAll(msgs)
        }
        for (msg in levelUpMessages) GameState.addLog(msg)
        GameState.addLog("Victory! +${expGained}EXP +${goldGained}G")

        val sb = StringBuilder()
        sb.append("EXP gained: $expGained\n")
        sb.append("Gold gained: $goldGained\n")
        if (levelUpMessages.isNotEmpty()) {
            sb.append("\n--- Level Up! ---\n")
            sb.append(levelUpMessages.joinToString("\n"))
        }

        AlertDialog.Builder(this)
            .setTitle("VICTORY!")
            .setMessage(sb.toString())
            .setCancelable(false)
            .setPositiveButton("Continue") { _, _ -> finish() }
            .show()
    }

    private fun showDefeatDialog() {
        battleOver = true
        SoundManager.defeat()
        tvTurnIndicator.text = "GAME OVER"
        tvTurnIndicator.setTextColor(Color.parseColor("#cc3333"))
        llMainActions.visibility = View.GONE
        GameState.addLog("The party was defeated on Floor ${GameState.currentFloor}...")

        AlertDialog.Builder(this)
            .setTitle("GAME OVER")
            .setMessage("Your party has been defeated...")
            .setCancelable(false)
            .setPositiveButton("Return to Title") { _, _ ->
                val intent = android.content.Intent(this, com.stormquest.MainActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun showFledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Escaped!")
            .setMessage("Your party escaped successfully!")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    // ========== UI HELPERS ==========

    private fun populateEnemyTargets(prompt: String, onSelect: (BattleEnemy) -> Unit) {
        llEnemyTargets.removeAllViews()
        addLabel(llEnemyTargets, prompt, "#ffd700")

        for (enemy in enemies.filter { it.isAlive }) {
            val btn = makeButton("${enemy.displayName}  HP:${enemy.currentHp}/${enemy.data.maxHp}", R.drawable.btn_enemy_target, true)
            btn.setOnClickListener { onSelect(enemy) }
            llEnemyTargets.addView(btn)
        }
    }

    private fun addLabel(container: LinearLayout, text: String, colorHex: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor(colorHex))
            textSize = 13f
            setPadding(4, 4, 4, 8)
        }
        container.addView(tv)
    }

    private fun makeButton(label: String, bgRes: Int, enabled: Boolean): Button {
        return Button(this).apply {
            text = label
            setBackgroundResource(bgRes)
            setTextColor(Color.parseColor("#e8e8e8"))
            textSize = 11f
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.4f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 4, 0, 4)
            layoutParams = lp
        }
    }

    private fun addBackButton(container: LinearLayout, onBack: () -> Unit) {
        val btn = Button(this).apply {
            text = "< Back"
            setBackgroundResource(R.drawable.btn_secondary)
            setTextColor(Color.parseColor("#aaaaaa"))
            textSize = 12f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 8, 0, 0)
            layoutParams = lp
        }
        btn.setOnClickListener { onBack() }
        container.addView(btn)
    }
}
