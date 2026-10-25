package com.example.invepulpe.view.movement

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.invepulpe.R
import com.example.invepulpe.controller.MovementController
import com.example.invepulpe.model.Movement
import com.example.invepulpe.model.MovementType
import com.example.invepulpe.model.Product
import com.example.invepulpe.view.BaseInventoryActivity

class MovementActivity : BaseInventoryActivity() {
    private val controller by lazy { MovementController(inventory) }
    private lateinit var adapter: MovementAdapter
    private var products: List<Product> = emptyList()
    private var types: List<MovementType> = emptyList()
    private var selectedProductId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen(R.layout.activity_movements, R.id.nav_movements)
        selectedProductId = savedInstanceState?.getLong("selected_product") ?: 0L
        adapter = MovementAdapter(this)
        findViewById<ListView>(R.id.movement_list).apply {
            adapter = this@MovementActivity.adapter
            emptyView = this@MovementActivity.findViewById(R.id.empty_movements)
            setOnItemClickListener { _, _, position, _ -> showOptions(this@MovementActivity.adapter.getItem(position)) }
        }
        findViewById<View>(R.id.register_movement).setOnClickListener { register() }
    }

    override fun onResume() { super.onResume(); refresh() }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("selected_product", selectedProduct()?.id ?: selectedProductId)
        super.onSaveInstanceState(outState)
    }

    private fun refresh() {
        selectedProductId = selectedProduct()?.id ?: selectedProductId
        load({ Triple(controller.products(), controller.types(), controller.list()) }) { data ->
            products = data.first
            types = data.second
            findViewById<Spinner>(R.id.select_product).apply {
                adapter = ArrayAdapter(this@MovementActivity, android.R.layout.simple_spinner_item,
                    products.map { getString(R.string.product_option, it.name, it.stock) }).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                setSelection(products.indexOfFirst { it.id == selectedProductId }.coerceAtLeast(0))
            }
            findViewById<Spinner>(R.id.select_type).adapter = typeAdapter()
            findViewById<View>(R.id.register_movement).isEnabled = products.isNotEmpty()
            findViewById<TextView>(R.id.movement_help).setText(
                if (products.isEmpty()) R.string.create_product_first else R.string.movement_help)
            adapter.submit(data.third, products)
        }
    }

    private fun typeAdapter() = ArrayAdapter(this, android.R.layout.simple_spinner_item, types).also {
        it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    private fun selectedProduct(): Product? =
        products.getOrNull(findViewById<Spinner>(R.id.select_product).selectedItemPosition)

    private fun register() {
        val product = selectedProduct() ?: return
        val type = types.getOrNull(findViewById<Spinner>(R.id.select_type).selectedItemPosition) ?: return
        val quantity = findViewById<EditText>(R.id.input_quantity).text.toString()
        load({ controller.register(product.id, type.id, quantity) }) { updated ->
            findViewById<EditText>(R.id.input_quantity).text.clear()
            refresh()
            showSaved(updated)
        }
    }

    private fun showOptions(movement: Movement) {
        val name = products.firstOrNull { it.id == movement.productId }?.name.orEmpty()
        AlertDialog.Builder(this).setTitle(name)
            .setItems(arrayOf(getString(R.string.edit_movement), getString(R.string.delete_movement))) { _, action ->
                if (action == 0) edit(movement) else confirmDelete(movement)
            }.show()
    }

    private fun edit(movement: Movement) {
        val form = layoutInflater.inflate(R.layout.dialog_movement, null)
        val type = form.findViewById<Spinner>(R.id.edit_type)
        type.adapter = typeAdapter()
        type.setSelection(types.indexOfFirst { it.id == movement.typeId }.coerceAtLeast(0))
        val quantity = form.findViewById<EditText>(R.id.edit_quantity)
        quantity.setText(movement.qty.toString())
        val dialog = AlertDialog.Builder(this).setTitle(R.string.edit_movement).setView(form)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val typeId = types.getOrNull(type.selectedItemPosition)?.id ?: return@setOnClickListener
                val qty = quantity.text.toString()
                load({ controller.edit(movement.id, typeId, qty) }) { updated ->
                    dialog.dismiss(); refresh(); showSaved(updated)
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(movement: Movement) {
        AlertDialog.Builder(this).setTitle(R.string.delete_movement)
            .setMessage(R.string.delete_movement_confirm).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                load({ controller.delete(movement.id) }) { updated -> refresh(); showSaved(updated) }
            }.show()
    }
}
