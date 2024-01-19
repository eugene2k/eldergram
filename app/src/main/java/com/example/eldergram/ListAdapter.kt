package com.example.eldergram


import android.content.res.Resources
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView


class ListAdapter : RecyclerView.Adapter<ListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun getItemView(): View { return this.itemView }
    }
    class Item(val name: String, val userId: Int)

    private val list: ArrayList<Item> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = layoutInflater.inflate(R.layout.list_item, parent,false)
        val phoneIcon = ResourcesCompat.getDrawable(Resources.getSystem(),android.R.drawable.sym_action_call, null)
        phoneIcon?.setTint(Color.WHITE)
        val button = ((layout as LinearLayout).getChildAt(1) as ImageButton)
        button.setImageDrawable(phoneIcon)
        button.setPadding(10)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val layout: LinearLayout = holder.getItemView() as LinearLayout
        val textView = layout.getChildAt(0) as TextView
        val item = list[position]
        textView.text = item.name
        val button = layout.getChildAt(1) as ImageButton
        button.setOnClickListener { println(item.userId) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun addItem(item: Item) {
        list.add(item)
    }
}