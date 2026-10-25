package com.example.invepulpe.view.product

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.example.invepulpe.R
import com.example.invepulpe.controller.ProductController
import com.example.invepulpe.model.Product
import com.example.invepulpe.view.BaseInventoryActivity
import com.example.invepulpe.view.DisplayFormat

open class ProductListActivity : BaseInventoryActivity() {
    private val controller by lazy { ProductController(inventory) }
    private lateinit var adapter: ProductAdapter
    private var products: List<Product> = emptyList()
    private var lowStockOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen(R.layout.activity_main, R.id.nav_products)
        lowStockOnly = savedInstanceState?.getBoolean("low_stock_only") ?: false
        adapter = ProductAdapter(this)
        findViewById<ListView>(R.id.product_list).apply {
            adapter = this@ProductListActivity.adapter
            emptyView = this@ProductListActivity.findViewById(R.id.empty_products)
            setOnItemClickListener { _, _, position, _ -> showOptions(this@ProductListActivity.adapter.getItem(position)) }
        }
        findViewById<View>(R.id.add_product).setOnClickListener { openForm() }
        findViewById<EditText>(R.id.search_products).doAfterTextChanged { render() }
        findViewById<View>(R.id.filter_all).setOnClickListener { lowStockOnly = false; render() }
        findViewById<View>(R.id.filter_low_stock).setOnClickListener { lowStockOnly = true; render() }
    }

    override fun onResume() { super.onResume(); refresh() }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("low_stock_only", lowStockOnly)
        super.onSaveInstanceState(outState)
    }

    private fun refresh() = load({ controller.list() }) { products = it; render() }

    private fun render() {
        val query = findViewById<EditText>(R.id.search_products).text.toString().trim()
        adapter.submit(products.filter {
            (!lowStockOnly || it.isLowStock) &&
                (it.name.contains(query, true) || it.barcode?.contains(query, true) == true)
        })
        findViewById<View>(R.id.filter_all).isSelected = !lowStockOnly
        findViewById<View>(R.id.filter_low_stock).isSelected = lowStockOnly
        findViewById<TextView>(R.id.inventory_total).text = DisplayFormat.money(products.sumOf { it.inventoryValueCents })
        findViewById<TextView>(R.id.product_count).text = resources.getQuantityString(R.plurals.product_count, products.size, products.size)
        findViewById<TextView>(R.id.stock_banner).apply {
            val count = products.count { it.isLowStock }
            visibility = if (count > 0) View.VISIBLE else View.GONE
            text = resources.getQuantityString(R.plurals.low_stock_count, count, count)
        }
        findViewById<TextView>(R.id.empty_products).setText(
            if (products.isEmpty()) R.string.no_products else R.string.no_results)
    }

    private fun openForm(id: Long = 0L) {
        startActivity(Intent(this, ProductFormActivity::class.java).putExtra(ProductFormActivity.PRODUCT_ID, id))
    }

    private fun showOptions(product: Product) {
        AlertDialog.Builder(this).setTitle(product.name)
            .setItems(arrayOf(getString(R.string.edit_product), getString(R.string.delete_product))) { _, action ->
                if (action == 0) openForm(product.id) else confirmDelete(product)
            }.show()
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(this).setTitle(R.string.delete_product)
            .setMessage(getString(R.string.delete_product_confirm, product.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                load({ controller.delete(product.id); controller.list() }) { products = it; render() }
            }.show()
    }
}
