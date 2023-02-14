package io.github.retrofitx.android.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.retrofitx.android.databinding.ItemProductBinding
import io.github.retrofitx.android.dto.Product

class ProductsAdapter(
    private val products: List<Product>,
    private val onProductClicked: (Product) -> Unit
): RecyclerView.Adapter<ProductViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding).apply {
            itemView.setOnClickListener {
                onProductClicked(products[adapterPosition])
            }
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size
}

class ProductViewHolder(private val binding: ItemProductBinding): RecyclerView.ViewHolder(binding.root) {

    fun bind(product: Product) {
        binding.price.text = product.price.toString()
        binding.name.text = product.name
    }
}