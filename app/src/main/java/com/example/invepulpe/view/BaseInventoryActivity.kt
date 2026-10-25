package com.example.invepulpe.view

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.invepulpe.MainActivity
import com.example.invepulpe.R
import com.example.invepulpe.data.database.InventoryDatabaseHelper
import com.example.invepulpe.model.Product
import com.example.invepulpe.service.InventoryService
import com.example.invepulpe.view.movement.MovementActivity
import com.example.invepulpe.view.summary.SummaryActivity
import java.util.concurrent.Executors

abstract class BaseInventoryActivity : AppCompatActivity() {
    protected val inventory by lazy { InventoryService(InventoryDatabaseHelper.getInstance(applicationContext)) }
    private val executor = Executors.newSingleThreadExecutor()
    private var running = false

    protected fun showScreen(layout: Int, selectedTab: Int = 0) {
        enableEdgeToEdge()
        setContentView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val destinations = mapOf(R.id.nav_products to MainActivity::class.java,
            R.id.nav_movements to MovementActivity::class.java, R.id.nav_summary to SummaryActivity::class.java)
        destinations.forEach { (id, target) ->
            findViewById<View>(id)?.apply {
                isSelected = id == selectedTab
                setOnClickListener {
                    if (id == selectedTab) return@setOnClickListener
                    startActivity(Intent(this@BaseInventoryActivity, target).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    if (this@BaseInventoryActivity !is MainActivity) finish()
                }
            }
        }
    }

    protected fun <T> load(work: () -> T, done: (T) -> Unit) {
        if (running) return
        running = true
        findViewById<ProgressBar>(R.id.progress)?.visibility = View.VISIBLE
        executor.execute {
            val result = runCatching(work)
            runOnUiThread {
                running = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                findViewById<ProgressBar>(R.id.progress)?.visibility = View.GONE
                result.fold(done) { error ->
                    Log.e("InvePulpe", "Inventory operation failed", error)
                    val message = when (error) {
                        is IllegalArgumentException -> error.message ?: getString(R.string.invalid_data)
                        is ArithmeticException -> getString(R.string.value_too_large)
                        else -> getString(R.string.database_error)
                    }
                    AlertDialog.Builder(this).setTitle(R.string.operation_failed).setMessage(message)
                        .setPositiveButton(R.string.accept, null).show()
                }
            }
        }
    }

    protected fun showSaved(product: Product, after: () -> Unit = {}) {
        if (product.isLowStock) {
            AlertDialog.Builder(this).setTitle(R.string.low_stock_title)
                .setMessage(getString(R.string.low_stock_message, product.name, product.stock, product.minStock))
                .setCancelable(false).setPositiveButton(R.string.accept) { _, _ -> after() }.show()
        } else {
            Toast.makeText(this, R.string.saved_successfully, Toast.LENGTH_SHORT).show()
            after()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
