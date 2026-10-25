package com.example.invepulpe.view.movement

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.invepulpe.R
import com.example.invepulpe.model.Movement
import com.example.invepulpe.model.MovementType
import com.example.invepulpe.model.Product
import com.example.invepulpe.view.DisplayFormat

class MovementAdapter(private val context: Context) : BaseAdapter() {
    private var movements: List<Movement> = emptyList()
    private var names: Map<Long, String> = emptyMap()
    fun submit(items: List<Movement>, products: List<Product>) {
        movements = items
        names = products.associate { it.id to it.name }
        notifyDataSetChanged()
    }
    override fun getCount() = movements.size
    override fun getItem(position: Int) = movements[position]
    override fun getItemId(position: Int) = getItem(position).id
    override fun hasStableIds() = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_movement, parent, false)
        val movement = getItem(position)
        val entry = movement.typeId == MovementType.ENTRY_ID
        view.findViewById<TextView>(R.id.movement_product).text = names[movement.productId]
        view.findViewById<TextView>(R.id.movement_date).text = DisplayFormat.date(movement.date)
        view.findViewById<TextView>(R.id.movement_quantity).apply {
            text = context.getString(if (entry) R.string.entry_quantity else R.string.exit_quantity, movement.qty)
            setTextColor(ContextCompat.getColor(context, if (entry) R.color.primary else R.color.danger))
        }
        return view
    }
}
