package com.example.invepulpe.service

import com.example.invepulpe.model.Movement
import com.example.invepulpe.model.MovementType
import com.example.invepulpe.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

object InventoryRules {
    const val MAX_STOCK = 1_000_000
    const val MAX_PRICE_CENTS = 100_000_000L

    fun priceCents(input: String): Long {
        val text = input.trim().replace(',', '.')
        require(Regex("[0-9]+(\\.[0-9]{1,2})?").matches(text)) {
            "Escribe un precio positivo con hasta dos decimales, por ejemplo 25.50."
        }
        val cents = try {
            BigDecimal(text).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        } catch (_: ArithmeticException) { throw IllegalArgumentException("El precio es demasiado grande.") }
        require(cents in 0..MAX_PRICE_CENTS) { "El precio máximo es L 1,000,000.00." }
        return cents
    }

    fun quantity(input: String, label: String, allowZero: Boolean = false): Int {
        val value = input.trim().toIntOrNull()
        require(value != null && value in (if (allowZero) 0 else 1)..MAX_STOCK) {
            "$label debe ser un número entero entre ${if (allowZero) 0 else 1} y 1,000,000."
        }
        return value
    }

    fun validate(product: Product) {
        require(product.name.trim().length in 1..100) { "El nombre debe tener entre 1 y 100 caracteres." }
        require(product.barcode == null || product.barcode.length in 1..64) {
            "El código de barra debe tener entre 1 y 64 caracteres."
        }
        require(product.priceCents in 0..MAX_PRICE_CENTS) { "El precio está fuera del rango permitido." }
        require(product.stock in 0..MAX_STOCK) { "El stock está fuera del rango permitido." }
        require(product.minStock in 0..MAX_STOCK) { "El stock mínimo está fuera del rango permitido." }
    }

    fun delta(typeId: Long, qty: Int): Int {
        require(qty in 1..MAX_STOCK) { "La cantidad debe estar entre 1 y 1,000,000." }
        return when (typeId) {
            MovementType.ENTRY_ID -> qty
            MovementType.EXIT_ID -> -qty
            else -> throw IllegalArgumentException("Selecciona un tipo de movimiento válido.")
        }
    }

    fun updatedStock(current: Int, delta: Int): Int {
        val result = current.toLong() + delta
        require(result >= 0) { "Stock insuficiente. Hay $current unidades disponibles." }
        require(result <= MAX_STOCK) { "El stock no puede superar 1,000,000 de unidades." }
        return result.toInt()
    }

    fun validateHistory(movements: List<Movement>) {
        var stock = 0
        movements.sortedWith(compareBy<Movement> { it.date }.thenBy { it.id }).forEach {
            val next = stock.toLong() + delta(it.typeId, it.qty)
            require(next in 0..MAX_STOCK.toLong()) {
                "Este cambio dejaría un saldo inválido en el historial. Revisa los movimientos posteriores."
            }
            stock = next.toInt()
        }
    }
}
