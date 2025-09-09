package com.example.multiurl.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.multiurl.R
import com.example.multiurl.data.UrlEntity

class UrlAdapter(
    private val onEdit: (UrlEntity) -> Unit,
    private val onDelete: (UrlEntity) -> Unit,
    private val onDefaultChange: (UrlEntity) -> Unit
) : RecyclerView.Adapter<UrlAdapter.VH>() {

    private val items = mutableListOf<UrlEntity>()

    fun submit(list: List<UrlEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getSelectedOrDefault(): UrlEntity? = items.firstOrNull { it.isDefault } ?: items.firstOrNull()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_url, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.txtUrl.text = item.url
        holder.checkDefault.setOnCheckedChangeListener(null)
        holder.checkDefault.isChecked = item.isDefault

        holder.checkDefault.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onDefaultChange(item)
        }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtUrl: TextView = v.findViewById(R.id.txtUrl)
        val checkDefault: CheckBox = v.findViewById(R.id.checkDefault)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }
}
