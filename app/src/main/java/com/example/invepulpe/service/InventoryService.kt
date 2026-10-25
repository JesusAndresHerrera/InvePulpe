package com.example.invepulpe.service

import android.database.sqlite.SQLiteDatabase
import com.example.invepulpe.data.dao.MovementDao
import com.example.invepulpe.data.dao.MovementTypeDao
import com.example.invepulpe.data.dao.ProductDao
import com.example.invepulpe.data.database.InventoryDatabaseHelper
import com.example.invepulpe.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class InventoryService(
    private val helper: InventoryDatabaseHelper,
    private val products: ProductDao = ProductDao(),
    private val movements: MovementDao = MovementDao(),
    private val types: MovementTypeDao = MovementTypeDao()
) {
    fun products(): List<Product> = products.all(helper.readableDatabase)
    fun product(id: Long): Product = products.find(helper.readableDatabase, id)
        ?: throw IllegalArgumentException("El producto ya no existe.")
    fun movementTypes(): List<MovementType> = types.all(helper.readableDatabase)
    fun movements(): List<Movement> = movements.all(helper.readableDatabase)

    fun saveProduct(input: Product): Product = transaction { db ->
        val product = input.copy(name = input.name.trim(), barcode = input.barcode?.trim()?.ifEmpty { null })
        InventoryRules.validate(product)
        require(product.barcode == null || !products.barcodeExists(db, product.barcode, product.id)) {
            "Ese código de barra ya pertenece a otro producto."
        }
        if (product.id == 0L) {
            val id = products.insert(db, product.copy(stock = 0))
            if (product.stock > 0) {
                movements.insert(db, Movement(productId = id, typeId = MovementType.ENTRY_ID,
                    qty = product.stock, date = now()))
                products.setStock(db, id, product.stock)
            }
            product.copy(id = id)
        } else {
            val current = products.find(db, product.id)
                ?: throw IllegalArgumentException("El producto ya no existe.")
            product.copy(stock = current.stock).also { products.update(db, it) }
        }
    }

    fun deleteProduct(id: Long) = transaction { db ->
        require(movements.forProduct(db, id).isEmpty()) {
            "Este producto tiene movimientos. Para conservar el historial no puede eliminarse."
        }
        requireNotNull(products.find(db, id)) { "El producto ya no existe." }
        products.delete(db, id)
    }

    fun recordMovement(productId: Long, typeId: Long, qty: Int): Product = transaction { db ->
        val delta = InventoryRules.delta(typeId, qty)
        val product = products.find(db, productId)
            ?: throw IllegalArgumentException("Selecciona un producto existente.")
        val stock = InventoryRules.updatedStock(product.stock, delta)
        // Keep timestamps monotonic even if the device clock is adjusted backwards.
        val latestDate = movements.forProduct(db, productId).lastOrNull()?.date
        val date = now().let { if (latestDate != null && it < latestDate) latestDate else it }
        movements.insert(db, Movement(productId = productId, typeId = typeId, qty = qty, date = date))
        products.setStock(db, productId, stock)
        product.copy(stock = stock)
    }

    fun editMovement(id: Long, typeId: Long, qty: Int): Product = transaction { db ->
        val old = movements.find(db, id) ?: throw IllegalArgumentException("El movimiento ya no existe.")
        val product = requireNotNull(products.find(db, old.productId))
        val change = InventoryRules.delta(typeId, qty) - InventoryRules.delta(old.typeId, old.qty)
        val stock = InventoryRules.updatedStock(product.stock, change)
        val updated = old.copy(typeId = typeId, qty = qty)
        InventoryRules.validateHistory(movements.forProduct(db, old.productId).map { if (it.id == id) updated else it })
        movements.update(db, updated)
        products.setStock(db, product.id, stock)
        product.copy(stock = stock)
    }

    fun deleteMovement(id: Long): Product = transaction { db ->
        val old = movements.find(db, id) ?: throw IllegalArgumentException("El movimiento ya no existe.")
        val product = requireNotNull(products.find(db, old.productId))
        val stock = InventoryRules.updatedStock(product.stock, -InventoryRules.delta(old.typeId, old.qty))
        InventoryRules.validateHistory(movements.forProduct(db, old.productId).filter { it.id != id })
        movements.delete(db, id)
        products.setStock(db, product.id, stock)
        product.copy(stock = stock)
    }

    fun summary(): InventorySummary {
        val all = products()
        val valuations = all.map { ProductValuation(it, it.inventoryValueCents) }
        return InventorySummary(
            totalValueCents = valuations.fold(0L) { total, item -> Math.addExact(total, item.valueCents) },
            topProducts = valuations.sortedWith(compareByDescending<ProductValuation> { it.valueCents }
                .thenBy { it.product.name.lowercase(Locale.ROOT) }.thenBy { it.product.id }).take(5),
            lowStockProducts = all.filter { it.isLowStock },
            productCount = all.size,
            totalUnits = all.sumOf { it.stock.toLong() }
        )
    }

    private fun <T> transaction(block: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val result = block(db)
            db.setTransactionSuccessful()
            return result
        } finally { db.endTransaction() }
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())
}
