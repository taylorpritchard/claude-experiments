package com.stormquest.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.stormquest.R
import com.stormquest.data.Item
import com.stormquest.data.Items
import com.stormquest.model.GameState

class ShopActivity : AppCompatActivity() {

    private lateinit var tvGold: TextView
    private lateinit var llShopItems: LinearLayout
    private lateinit var llInventory: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        tvGold = findViewById(R.id.tvShopGold)
        llShopItems = findViewById(R.id.llShopItems)
        llInventory = findViewById(R.id.llInventory)

        val btnBack = findViewById<Button>(R.id.btnShopBack)
        btnBack.setOnClickListener { finish() }

        buildShopItems()
        updateInventory()
        updateGold()
    }

    private fun buildShopItems() {
        llShopItems.removeAllViews()
        for (item in Items.SHOP_ITEMS) {
            val row = buildShopRow(item)
            llShopItems.addView(row)
        }
    }

    private fun buildShopRow(item: Item): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 12, 8, 12)
            setBackgroundColor(Color.parseColor("#16213e"))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 4, 0, 4)
            layoutParams = lp
        }

        val tvInfo = TextView(this).apply {
            text = "${item.displayName}\n${item.description}"
            setTextColor(Color.parseColor("#e8e8e8"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
        }

        val tvPrice = TextView(this).apply {
            text = "${item.buyPrice}G"
            setTextColor(Color.parseColor("#ffd700"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(8, 0, 8, 0)
        }

        val btnBuy = Button(this).apply {
            text = "Buy"
            setBackgroundResource(R.drawable.btn_primary)
            setTextColor(Color.parseColor("#0d0d1a"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        btnBuy.setOnClickListener { buyItem(item) }

        row.addView(tvInfo)
        row.addView(tvPrice)
        row.addView(btnBuy)
        return row
    }

    private fun buyItem(item: Item) {
        if (GameState.gold < item.buyPrice) {
            Toast.makeText(this, "Not enough gold! (Need ${item.buyPrice}G)", Toast.LENGTH_SHORT).show()
            return
        }
        GameState.gold -= item.buyPrice
        GameState.addItem(item.id)
        updateGold()
        updateInventory()
        Toast.makeText(this, "Purchased ${item.displayName}!", Toast.LENGTH_SHORT).show()
    }

    private fun updateGold() {
        tvGold.text = "Gold: ${GameState.gold}G"
    }

    private fun updateInventory() {
        llInventory.removeAllViews()

        if (GameState.inventory.isEmpty()) {
            val tv = TextView(this).apply {
                text = "(empty)"
                setTextColor(Color.parseColor("#aaaaaa"))
                textSize = 13f
                setPadding(8, 8, 8, 8)
            }
            llInventory.addView(tv)
            return
        }

        for ((itemId, qty) in GameState.inventory) {
            val item = Items.ALL.firstOrNull { it.id == itemId } ?: continue
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
            }
            val tvItem = TextView(this).apply {
                text = "${item.displayName} x$qty"
                setTextColor(Color.parseColor("#e8e8e8"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvItem)
            llInventory.addView(row)
        }
    }
}
