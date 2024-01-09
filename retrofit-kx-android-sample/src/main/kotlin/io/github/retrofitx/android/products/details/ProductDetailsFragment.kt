package io.github.retrofitx.android.products.details

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.retrofitx.android.R
import io.github.retrofitx.android.databinding.LayoutProductDetailsBinding
import io.github.retrofitx.android.utils.showToast
import io.github.retrofitx.android.utils.viewBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProductDetailsFragment: Fragment(R.layout.layout_product_details) {
    private val viewModel by viewModel<ProductDetailsViewModel>()
    private val binding by viewBinding(LayoutProductDetailsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setup()
        viewModel.observeEvents()
    }

    private fun LayoutProductDetailsBinding.setup() {
        name.text = viewModel.product.name
        price.text = viewModel.product.price.toString()
        delete.setOnClickListener { showConfirmationDialog() }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.confirm_title)
            setMessage(getString(R.string.product_deletion_message, viewModel.product.name))
            setPositiveButton(R.string.ok) { _, _ -> viewModel.deleteProduct() }
            setNegativeButton(R.string.cancel, null)
        }.show()
    }

    private fun ProductDetailsViewModel.observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            for (event in events) {
                when (event) {
                    is ProductDetailsEvent.ShowConnectionErrorMessage -> {
                        requireContext().showToast(R.string.connection_error)
                    }
                    is ProductDetailsEvent.ShowApiErrorMessage -> {
                        requireContext().showToast(getString(R.string.api_error, event.error.id.toString()))
                    }
                }
            }
        }
    }
}