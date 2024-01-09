package io.github.retrofitx.android.shops.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.retrofitx.android.R
import io.github.retrofitx.android.databinding.LayoutShopDetailsBinding
import io.github.retrofitx.android.utils.showToast
import io.github.retrofitx.android.utils.viewBinding

import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ShopDetailsFragment : Fragment(R.layout.layout_shop_details) {
    private val viewModel by viewModel<ShopDetailsViewModel>()
    private val binding by viewBinding(LayoutShopDetailsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setup()
        viewModel.observeEvents()
    }

    private fun LayoutShopDetailsBinding.setup() {
        shopId.text = viewModel.shop.id.toString()
        shopName.text = viewModel.shop.name
        delete.setOnClickListener { viewModel.deleteShop() }
    }

    private fun ShopDetailsViewModel.observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            for (event in events) {
                when (event) {
                    is ShopDetailsEvent.ShowConnectionErrorMessage -> {
                        requireContext().showToast(R.string.connection_error)
                    }
                    is ShopDetailsEvent.ShowApiErrorMessage -> {
                        requireContext().showToast(getString(R.string.api_error, event.error.message))
                    }
                }
            }
        }
    }
}