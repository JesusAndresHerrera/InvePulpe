package com.example.invepulpe.view.summary

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.invepulpe.R
import com.example.invepulpe.controller.SummaryController
import com.example.invepulpe.view.BaseInventoryActivity
import com.example.invepulpe.view.DisplayFormat

class SummaryActivity : BaseInventoryActivity() {
    private val controller by lazy { SummaryController(inventory) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen(R.layout.activity_summary, R.id.nav_summary)
    }

    override fun onResume() {
        super.onResume()
        load({ controller.load() }) { summary ->
            findViewById<TextView>(R.id.total_value).text = DisplayFormat.money(summary.totalValueCents)
            findViewById<TextView>(R.id.summary_counts).text = getString(R.string.summary_counts,
                summary.productCount, summary.totalUnits)
            findViewById<LinearLayout>(R.id.top_products).apply {
                removeAllViews()
                summary.topProducts.forEachIndexed { index, item ->
                    val row = layoutInflater.inflate(R.layout.item_valuation, this, false)
                    row.findViewById<TextView>(R.id.valuation_rank).text = (index + 1).toString()
                    row.findViewById<TextView>(R.id.valuation_name).text = item.product.name
                    row.findViewById<TextView>(R.id.valuation_detail).text = getString(R.string.valuation_detail,
                        item.product.stock, DisplayFormat.money(item.product.priceCents))
                    row.findViewById<TextView>(R.id.valuation_value).text = DisplayFormat.money(item.valueCents)
                    addView(row)
                }
            }
            findViewById<View>(R.id.empty_top).visibility = if (summary.topProducts.isEmpty()) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.low_stock_list).apply {
                val needsStock = summary.lowStockProducts.isNotEmpty()
                setBackgroundResource(if (needsStock) R.drawable.bg_warning else R.drawable.bg_card)
                setTextColor(ContextCompat.getColor(this@SummaryActivity,
                    if (needsStock) R.color.warning_text else R.color.text_secondary))
                text = when {
                    summary.productCount == 0 -> getString(R.string.empty_top)
                    !needsStock -> getString(R.string.stock_healthy)
                    else -> summary.lowStockProducts.joinToString("\n\n") {
                        getString(R.string.low_stock_row, it.name, it.stock, it.minStock)
                    }
                }
            }
        }
    }
}
