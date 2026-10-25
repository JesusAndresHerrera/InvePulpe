package com.example.invepulpe

import com.example.invepulpe.model.Movement
import com.example.invepulpe.model.Product
import com.example.invepulpe.service.InventoryRules
import org.junit.Assert.*
import org.junit.Test

class InventoryRulesTest {
    @Test fun pricesUseExactCents() {
        assertEquals(2550L, InventoryRules.priceCents("25.50"))
        assertEquals(10L, InventoryRules.priceCents("0,10"))
        assertEquals(0L, InventoryRules.priceCents("0"))
    }

    @Test fun rejectsInvalidPrices() {
        listOf("", "-1", "1.001", "abc", "1e3", "1,2.3", "1000000.01").forEach {
            assertThrows(IllegalArgumentException::class.java) { InventoryRules.priceCents(it) }
        }
    }

    @Test fun stockCannotBecomeNegative() {
        assertThrows(IllegalArgumentException::class.java) { InventoryRules.updatedStock(3, -4) }
    }

    @Test fun canSellExactlyAvailableStock() { assertEquals(0, InventoryRules.updatedStock(3, -3)) }

    @Test fun stockCannotOverflowLimit() {
        assertThrows(IllegalArgumentException::class.java) { InventoryRules.updatedStock(1_000_000, 1) }
    }

    @Test fun quantityMustBePositiveInteger() {
        listOf("0", "-1", "1.5", "1000001", "999999999999999").forEach {
            assertThrows(IllegalArgumentException::class.java) { InventoryRules.quantity(it, "Cantidad") }
        }
        assertEquals(0, InventoryRules.quantity("0", "Stock", true))
    }

    @Test fun rejectsUnknownMovementType() {
        assertThrows(IllegalArgumentException::class.java) { InventoryRules.delta(99, 1) }
    }

    @Test fun alertIncludesMinimumBoundaryAndZero() {
        assertTrue(Product(name = "Arroz", priceCents = 100, stock = 5, minStock = 5).isLowStock)
        assertTrue(Product(name = "Arroz", priceCents = 100, stock = 0, minStock = 0).isLowStock)
        assertFalse(Product(name = "Arroz", priceCents = 100, stock = 6, minStock = 5).isLowStock)
    }

    @Test fun valuationDoesNotUseFloatingPoint() {
        assertEquals(30L, Product(name = "Dulce", priceCents = 10, stock = 3).inventoryValueCents)
    }

    @Test fun historyCannotHaveNegativeIntermediateBalance() {
        val history = listOf(
            Movement(1, 1, 1, 2, "2026-10-24T10:00:00.000Z"),
            Movement(2, 1, 2, 3, "2026-10-24T11:00:00.000Z"),
            Movement(3, 1, 1, 5, "2026-10-24T12:00:00.000Z")
        )
        assertThrows(IllegalArgumentException::class.java) { InventoryRules.validateHistory(history) }
    }

    @Test fun equalTimestampUsesInsertionOrder() {
        InventoryRules.validateHistory(listOf(
            Movement(2, 1, 2, 2, "2026-10-24T10:00:00.000Z"),
            Movement(1, 1, 1, 2, "2026-10-24T10:00:00.000Z")
        ))
    }

    @Test fun rejectsBlankNameAndInvalidMinimum() {
        assertThrows(IllegalArgumentException::class.java) {
            InventoryRules.validate(Product(name = "  ", priceCents = 100))
        }
        assertThrows(IllegalArgumentException::class.java) {
            InventoryRules.validate(Product(name = "Arroz", priceCents = 100, minStock = -1))
        }
    }
}
