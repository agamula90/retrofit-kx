package com.github.retrofitx.android.products

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.retrofitx.android.R
import com.github.retrofitx.android.databinding.ProductsBinding
import com.github.retrofitx.android.utils.RecyclerViewHorizontalSpaceDecoration
import com.github.retrofitx.android.utils.RecyclerViewVerticalSpaceDecoration
import com.github.retrofitx.android.utils.showToast
import com.github.retrofitx.android.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductsFragment: Fragment(R.layout.products) {
    private val viewModel by viewModels<ProductsViewModel>()
    private val binding by viewBinding(ProductsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setup()
        viewModel.observeChanges()
    }

    private fun ProductsBinding.setup() {
        products.addItemDecoration(RecyclerViewVerticalSpaceDecoration())
        products.addItemDecoration(RecyclerViewHorizontalSpaceDecoration())
        binding.reload.setOnClickListener { viewModel.loadProducts() }
    }

    private fun ProductsViewModel.observeChanges() {
        products.observe(viewLifecycleOwner) { products ->
            binding.products.adapter = ProductsAdapter(
                products = products,
                onProductClicked = viewModel::goProductDetails
            )
        }
        isParseFailed.observe(viewLifecycleOwner) { isParseFailed ->
            binding.parseFailedMessage.isVisible = isParseFailed
        }
        observeEvents()
    }

    private fun ProductsViewModel.observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            for (event in events) {
                when(event) {
                    is ProductEvent.ShowConnectionErrorMessage -> {
                        requireContext().showToast(R.string.connection_error)
                    }
                    is ProductEvent.ShowApiErrorMessage -> {
                        requireContext().showToast(getString(R.string.api_error, event.error.id.toString()))
                    }
                }
            }
        }
    }
}