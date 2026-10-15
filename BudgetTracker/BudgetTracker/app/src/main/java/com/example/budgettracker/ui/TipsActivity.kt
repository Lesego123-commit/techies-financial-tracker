package com.example.budgettracker.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.budgettracker.databinding.ActivityTipsBinding
import kotlin.random.Random

/**
 * TipsActivity – OWN FEATURE: Budgeting Tips.
 *
 * Provides the user with practical budgeting advice. A featured "tip of the day" is shown at
 * the top with a button that shuffles to a new random tip, and the full list of tips is shown
 * below, grouped by topic. The tips are stored locally in the app so they work offline, which
 * matches the design document's description of this feature.
 */
class TipsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTipsBinding

    /** A single budgeting tip belonging to a topic. */
    private data class BudgetingTip(val topic: String, val text: String)

    // Local, offline list of budgeting tips grouped by topic.
    private val tips = listOf(
        BudgetingTip("Saving Strategies", "Try the 50/30/20 rule: 50% of income to needs, 30% to wants, and 20% to savings."),
        BudgetingTip("Saving Strategies", "Pay yourself first — move money to savings the day you get paid, before you spend."),
        BudgetingTip("Saving Strategies", "Automate a small monthly transfer to savings so it happens without you thinking about it."),
        BudgetingTip("Reducing Debt", "Pay more than the minimum on debt — even a little extra cuts the interest you pay."),
        BudgetingTip("Reducing Debt", "Tackle the highest-interest debt first (the avalanche method) to save the most money."),
        BudgetingTip("Grocery Budgeting", "Shop with a list and a full stomach — both reduce impulse buys at the till."),
        BudgetingTip("Grocery Budgeting", "Plan meals around what's on special and buy staples in bulk when they're cheap."),
        BudgetingTip("Everyday Spending", "Wait 24 hours before any non-essential purchase. If you still want it tomorrow, buy it."),
        BudgetingTip("Everyday Spending", "Track every expense for a month — you can't manage what you don't measure."),
        BudgetingTip("Emergency Funds", "Aim to build an emergency fund covering 3 months of essential expenses."),
        BudgetingTip("Emergency Funds", "Keep your emergency fund separate from your everyday account so you're not tempted."),
        BudgetingTip("Goal Setting", "Set a clear minimum and maximum monthly budget, then check your progress weekly.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Budgeting Tips"

        showRandomTip()
        binding.btnRandomTip.setOnClickListener { showRandomTip() }
        buildTipList()
    }

    /** Pick a random tip and display it in the featured card. */
    private fun showRandomTip() {
        val tip = tips[Random.nextInt(tips.size)]
        binding.tvFeaturedTip.text = "\"${tip.text}\""
    }

    /** Build the full tip list grouped by topic, adding views programmatically. */
    private fun buildTipList() {
        val container = binding.llTipsContainer
        container.removeAllViews()

        // Group tips by their topic and add a header + a card per tip.
        tips.groupBy { it.topic }.forEach { (topic, topicTips) ->
            container.addView(makeTopicHeader(topic))
            topicTips.forEach { tip ->
                container.addView(makeTipCard(tip.text))
            }
        }
    }

    /** Create a bold topic header TextView. */
    private fun makeTopicHeader(topic: String): TextView {
        return TextView(this).apply {
            text = topic
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(12), 0, dp(6))
            layoutParams = lp
        }
    }

    /** Create a simple card containing one tip's text. */
    private fun makeTipCard(text: String): CardView {
        val card = CardView(this).apply {
            radius = dp(10).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(4), 0, dp(4))
            layoutParams = lp
        }
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#424242"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.START
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        card.addView(tv)
        return card
    }

    /** Convert dp to pixels for the programmatic views. */
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
