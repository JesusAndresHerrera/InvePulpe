package com.example.invepulpe.view.product

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.invepulpe.R
import com.example.invepulpe.controller.ProductController
import com.example.invepulpe.view.BaseInventoryActivity
import com.example.invepulpe.view.DisplayFormat

class ProductFormActivity : BaseInventoryActivity() {
    private val controller by lazy { ProductController(inventory) }
    private val productId by lazy { intent.getLongExtra(PRODUCT_ID, 0L) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen(R.layout.activity_product_form)
        findViewById<View>(R.id.back_button).setOnClickListener { finish() }
        findViewById<View>(R.id.save_product).setOnClickListener { save() }
        if (productId != 0L) {
            findViewById<TextView>(R.id.form_title).setText(R.string.edit_product)
            findViewById<EditText>(R.id.input_stock).isEnabled = false
            findViewById<TextView>(R.id.stock_help).setText(R.string.stock_edit_help)
            if (savedInstanceState == null) load({ controller.find(productId) }) { product ->
                field(R.id.input_name).setText(product.name)
                field(R.id.input_barcode).setText(product.barcode.orEmpty())
                field(R.id.input_price).setText(DisplayFormat.priceInput(product.priceCents))
                field(R.id.input_stock).setText(product.stock.toString())
                field(R.id.input_min_stock).setText(product.minStock.toString())
            }
        }
    }

    private fun field(id: Int) = findViewById<EditText>(id)
    private fun text(id: Int) = field(id).text.toString()

    private fun save() {
        val name = text(R.id.input_name)
        val barcode = text(R.id.input_barcode)
        val price = text(R.id.input_price)
        val stock = text(R.id.input_stock)
        val minimum = text(R.id.input_min_stock)
        load({ controller.save(productId, name, barcode, price, stock, minimum) }) { product ->
            showSaved(product) { finish() }
        }
    }

    companion object { const val PRODUCT_ID = "product_id" }
}
