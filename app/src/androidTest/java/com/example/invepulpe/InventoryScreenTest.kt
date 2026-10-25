package com.example.invepulpe

import android.content.Intent
import android.os.SystemClock
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.invepulpe.data.database.InventoryDatabaseHelper
import com.example.invepulpe.model.MovementType
import com.example.invepulpe.service.InventoryService
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryScreenTest {
    @Test fun createProductSellAndSeeLowStockAndSummary() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val service = InventoryService(InventoryDatabaseHelper.getInstance(context))
        val name = "Arroz UI ${System.currentTimeMillis()}"
        context.startActivity(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        instrumentation.waitForIdleSync()
        try {
            onView(withId(R.id.add_product)).perform(click())
            onView(withId(R.id.input_name)).perform(replaceText(name), closeSoftKeyboard())
            onView(withId(R.id.input_barcode)).perform(scrollTo(), replaceText("UI${System.currentTimeMillis()}"), closeSoftKeyboard())
            onView(withId(R.id.input_price)).perform(scrollTo(), replaceText("25.50"), closeSoftKeyboard())
            onView(withId(R.id.input_stock)).perform(scrollTo(), replaceText("10"), closeSoftKeyboard())
            onView(withId(R.id.input_min_stock)).perform(scrollTo(), replaceText("5"), closeSoftKeyboard())
            onView(withId(R.id.save_product)).perform(scrollTo(), click())
            await { onView(withText(name)).check(matches(isDisplayed())) }
            onView(withId(R.id.filter_low_stock)).perform(click())
            onView(withId(R.id.filter_low_stock)).check(matches(isSelected()))
            onView(withText(name)).check(doesNotExist())
            onView(withId(R.id.filter_all)).perform(click())
            onView(withText(name)).check(matches(isDisplayed()))
            onView(withId(R.id.nav_movements)).perform(click())
            await { onView(withId(R.id.register_movement)).check(matches(isEnabled())) }
            onView(withId(R.id.select_product)).perform(scrollTo(), click())
            onData(containsString(name)).perform(click())
            onView(withId(R.id.select_type)).perform(scrollTo(), click())
            onData(`is`(MovementType(2, "SALIDA"))).perform(click())
            onView(withId(R.id.input_quantity)).perform(scrollTo(), replaceText("6"), closeSoftKeyboard())
            onView(withId(R.id.register_movement)).perform(scrollTo(), click())
            await { onView(withText(R.string.low_stock_title)).check(matches(isDisplayed())) }
            onView(withText(R.string.accept)).perform(click())
            onView(withId(R.id.nav_summary)).perform(click())
            await { onView(withId(R.id.total_value)).check(matches(withText("L 102.00"))) }
            onView(withText(name)).check(matches(isDisplayed()))
            assertEquals(4, service.products().single { it.name == name }.stock)
            onView(withId(R.id.nav_products)).perform(click())
            await { onView(withText(name)).check(matches(isDisplayed())) }
            onView(withId(R.id.filter_low_stock)).perform(click())
            onView(withText(name)).check(matches(isDisplayed()))
        } finally {
            // Only remove records created by this test, preserving any other inventory.
            service.products().filter { it.name == name }.forEach { product ->
                service.movements().filter { it.productId == product.id }.forEach { service.deleteMovement(it.id) }
                service.deleteProduct(product.id)
            }
        }
    }

    private fun await(check: () -> Unit) {
        val timeout = SystemClock.uptimeMillis() + 8_000
        var lastError: Throwable? = null
        while (SystemClock.uptimeMillis() < timeout) {
            try { check(); return } catch (error: AssertionError) { lastError = error }
            catch (error: androidx.test.espresso.NoMatchingViewException) { lastError = error }
            SystemClock.sleep(100)
        }
        throw AssertionError("La pantalla no mostró el resultado esperado.", lastError)
    }
}
