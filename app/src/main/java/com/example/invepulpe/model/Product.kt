package com.example.invepulpe.model

data class Product(
    val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val priceCents: Long,
    val stock: Int = 0,
    val minStock: Int = 5
) {
    val isLowStock: Boolean get() = stock <= minStock
    val inventoryValueCents: Long get() = Math.multiplyExact(priceCents, stock.toLong())
}
