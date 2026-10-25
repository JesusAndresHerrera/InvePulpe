package com.example.invepulpe

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.invepulpe.data.database.InventoryDatabaseHelper
import com.example.invepulpe.model.Product
import com.example.invepulpe.service.InventoryService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class InventoryDatabaseTest {
    private lateinit var helper: InventoryDatabaseHelper
    private lateinit var inventory: InventoryService

    @Before fun setup() {
        helper = InventoryDatabaseHelper.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)
        inventory = InventoryService(helper)
    }
    @After fun close() { helper.close() }

    private fun product(stock: Int = 10, barcode: String? = "00123") = inventory.saveProduct(
        Product(name = "Arroz", barcode = barcode, priceCents = 2550, stock = stock, minStock = 5))

    @Test fun seedsTypesAndEnablesForeignKeys() {
        assertEquals(listOf("ENTRADA", "SALIDA"), inventory.movementTypes().map { it.name })
        helper.readableDatabase.rawQuery("PRAGMA foreign_keys", null).use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
        assertThrows(SQLiteConstraintException::class.java) {
            helper.writableDatabase.execSQL("INSERT INTO movements(product_id,type_id,qty,date) VALUES(999,1,1,'2026-10-24T00:00:00.000Z')")
        }
    }

    @Test fun initialStockIsRecordedAsEntry() {
        val product = product()
        assertEquals(10, inventory.product(product.id).stock)
        assertEquals(10, inventory.movements().single().qty)
        assertEquals(1L, inventory.movements().single().typeId)
        assertEquals("00123", inventory.product(product.id).barcode)
    }

    @Test fun entryAndExitUpdateStock() {
        val product = product()
        inventory.recordMovement(product.id, 1, 3)
        val updated = inventory.recordMovement(product.id, 2, 8)
        assertEquals(5, updated.stock)
        assertTrue(updated.isLowStock)
        assertEquals(3, inventory.movements().size)
    }

    @Test fun oversellingDoesNotWriteAnything() {
        val product = product()
        assertThrows(IllegalArgumentException::class.java) { inventory.recordMovement(product.id, 2, 11) }
        assertEquals(10, inventory.product(product.id).stock)
        assertEquals(1, inventory.movements().size)
    }

    @Test fun failedMovementRollsBackStock() {
        val product = product()
        helper.writableDatabase.execSQL("CREATE TRIGGER reject_movement BEFORE INSERT ON movements BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        assertThrows(SQLiteException::class.java) { inventory.recordMovement(product.id, 2, 1) }
        assertEquals(10, inventory.product(product.id).stock)
        assertEquals(1, inventory.movements().size)
    }

    @Test fun failedInitialEntryRollsBackProduct() {
        helper.writableDatabase.execSQL("CREATE TRIGGER reject_movement BEFORE INSERT ON movements BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        assertThrows(SQLiteException::class.java) { product() }
        assertTrue(inventory.products().isEmpty())
    }

    @Test fun failedStockUpdateRollsBackInsertedMovement() {
        val product = product()
        helper.writableDatabase.execSQL("CREATE TRIGGER reject_stock BEFORE UPDATE OF stock ON products BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        assertThrows(SQLiteException::class.java) { inventory.recordMovement(product.id, 2, 1) }
        assertEquals(10, inventory.product(product.id).stock)
        assertEquals(1, inventory.movements().size)
    }

    @Test fun inventoryPersistsAfterClosingAndReopeningDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "persistence-test-${System.nanoTime()}.db"
        try {
            val firstHelper = InventoryDatabaseHelper(context, databaseName)
            val first = InventoryService(firstHelper)
            val product = first.saveProduct(Product(name = "Persistente", priceCents = 1250, stock = 8))
            first.recordMovement(product.id, 2, 3)
            firstHelper.close()
            val reopenedHelper = InventoryDatabaseHelper(context, databaseName)
            try {
                val reopened = InventoryService(reopenedHelper)
                assertEquals(5, reopened.product(product.id).stock)
                assertEquals(2, reopened.movements().size)
                assertEquals(6250L, reopened.summary().totalValueCents)
            } finally { reopenedHelper.close() }
        } finally { context.deleteDatabase(databaseName) }
    }

    @Test fun barcodeMustBeUniqueButCanBeOmitted() {
        product()
        assertThrows(IllegalArgumentException::class.java) { product() }
        product(0, null)
        product(0, null)
        assertEquals(3, inventory.products().size)
    }

    @Test fun editingDetailsPreservesCurrentStock() {
        val product = product()
        inventory.recordMovement(product.id, 2, 2)
        val updated = inventory.saveProduct(product.copy(name = "Arroz integral", stock = 999, priceCents = 3000))
        assertEquals(8, updated.stock)
        assertEquals("Arroz integral", inventory.product(product.id).name)
        assertEquals(24000L, inventory.summary().totalValueCents)
    }

    @Test fun productWithHistoryCannotBeDeleted() {
        val product = product()
        assertThrows(IllegalArgumentException::class.java) { inventory.deleteProduct(product.id) }
        assertEquals(product.id, inventory.product(product.id).id)
    }

    @Test fun productWithoutHistoryCanBeDeleted() {
        val product = product(0)
        inventory.deleteProduct(product.id)
        assertTrue(inventory.products().isEmpty())
    }

    @Test fun editingAndDeletingExitRestoresStock() {
        val product = product()
        inventory.recordMovement(product.id, 2, 4)
        val exit = inventory.movements().first()
        assertEquals(8, inventory.editMovement(exit.id, 2, 2).stock)
        assertEquals(10, inventory.deleteMovement(exit.id).stock)
    }

    @Test fun cannotRemoveEntryNeededByLaterExit() {
        val product = product(10)
        val initial = inventory.movements().single()
        inventory.recordMovement(product.id, 2, 10)
        inventory.recordMovement(product.id, 1, 20)
        assertThrows(IllegalArgumentException::class.java) { inventory.deleteMovement(initial.id) }
        assertThrows(IllegalArgumentException::class.java) { inventory.editMovement(initial.id, 1, 5) }
        assertEquals(20, inventory.product(product.id).stock)
        assertEquals(3, inventory.movements().size)
    }

    @Test fun summaryTotalsAndTopFiveAreCorrect() {
        (1..7).forEach { index -> inventory.saveProduct(
            Product(name = "Producto $index", priceCents = index * 100L, stock = index, minStock = 2)) }
        val summary = inventory.summary()
        assertEquals(14000L, summary.totalValueCents)
        assertEquals(28L, summary.totalUnits)
        assertEquals(7, summary.productCount)
        assertEquals(listOf("Producto 7", "Producto 6", "Producto 5", "Producto 4", "Producto 3"),
            summary.topProducts.map { it.product.name })
        assertEquals(2, summary.lowStockProducts.size)
    }

    @Test fun concurrentExitsCannotOversell() {
        val product = product(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results = executor.invokeAll(List(2) { Callable {
                runCatching { inventory.recordMovement(product.id, 2, 1) }.isSuccess
            } }).map { it.get() }
            assertEquals(1, results.count { it })
            assertEquals(0, inventory.product(product.id).stock)
            assertEquals(2, inventory.movements().size)
        } finally { executor.shutdown() }
    }
}
