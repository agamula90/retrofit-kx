package com.github.retrofitx.android.shops

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.retrofitx.android.databinding.ItemShopBinding
import com.github.retrofitx.android.dto.Shop

class ShopsAdapter(
    private val shops: List<Shop>,
    private val onShopClicked: (Shop) -> Unit
): RecyclerView.Adapter<ShopViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding).apply {
            itemView.setOnClickListener {
                onShopClicked(shops[adapterPosition])
            }
        }
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(shops[position])
    }

    override fun getItemCount(): Int = shops.size
}

class ShopViewHolder(private val binding: ItemShopBinding): RecyclerView.ViewHolder(binding.root) {

    fun bind(shop: Shop) {
        binding.shopId.text = shop.id.toString()
        binding.shopName.text = shop.name
    }
}