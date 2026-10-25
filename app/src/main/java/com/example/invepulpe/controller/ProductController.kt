package com.example.invepulpe.controller

import com.example.invepulpe.model.Product
import com.example.invepulpe.service.InventoryRules
import com.example.invepulpe.service.InventoryService

class ProductController(private val inventory: InventoryService) {
    fun list(): List<Product> = inventory.products()
    fun find(id: Long): Product = inventory.product(id)
    fun delete(id: Long) = inventory.deleteProduct(id)

    fun save(id: Long, name: String, barcode: String, price: String, initialStock: String, minimum: String): Product {
        val stock = if (id == 0L) InventoryRules.quantity(initialStock, "El stock inicial", true) else 0
        return inventory.saveProduct(Product(id, name, barcode.ifBlank { null },
            InventoryRules.priceCents(price), stock, InventoryRules.quantity(minimum, "El stock mínimo", true)))
    }
}
