package com.example.invepulpe.view.product

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.invepulpe.R
import com.example.invepulpe.model.Product
import com.example.invepulpe.view.DisplayFormat

class ProductAdapter(private val context: Context) : BaseAdapter() {
    private var products: List<Product> = emptyList()
    fun submit(items: List<Product>) { products = items; notifyDataSetChanged() }
    override fun getCount() = products.size
    override fun getItem(position: Int) = products[position]
    override fun getItemId(position: Int) = getItem(position).id
    override fun hasStableIds() = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_product, parent, false)
        val product = getItem(position)
        view.findViewById<TextView>(R.id.product_name).text = product.name
        view.findViewById<TextView>(R.id.product_price).text = DisplayFormat.money(product.priceCents)
        view.findViewById<TextView>(R.id.product_barcode).text = product.barcode ?: context.getString(R.string.no_barcode)
        view.findViewById<TextView>(R.id.product_stock).apply {
            text = context.getString(R.string.stock_number, product.stock)
            setTextColor(ContextCompat.getColor(context,
                if (product.isLowStock) R.color.warning_text else R.color.text_primary))
        }
        view.findViewById<TextView>(R.id.product_warning).apply {
            setText(if (product.isLowStock) R.string.low_stock_badge else R.string.units_short)
            setTextColor(ContextCompat.getColor(context,
                if (product.isLowStock) R.color.warning_text else R.color.text_secondary))
        }
        return view
    }
}
