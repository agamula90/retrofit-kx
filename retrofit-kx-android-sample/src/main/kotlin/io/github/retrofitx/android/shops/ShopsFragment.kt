package io.github.retrofitx.android.shops

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.retrofitx.android.R
import io.github.retrofitx.android.databinding.ShopsBinding
import io.github.retrofitx.android.utils.RecyclerViewHorizontalSpaceDecoration
import io.github.retrofitx.android.utils.RecyclerViewVerticalSpaceDecoration
import io.github.retrofitx.android.utils.showToast
import io.github.retrofitx.android.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShopsFragment: Fragment(R.layout.shops) {
    private val viewModel by viewModels<ShopsViewModel>()
    private val binding by viewBinding(ShopsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setup()
        viewModel.observeChanges()
    }

    private fun ShopsBinding.setup() {
        shops.addItemDecoration(RecyclerViewVerticalSpaceDecoration())
        shops.addItemDecoration(RecyclerViewHorizontalSpaceDecoration())
        binding.reload.setOnClickListener { viewModel.loadShops() }
    }

    private fun ShopsViewModel.observeChanges() {
        shops.observe(viewLifecycleOwner) { shops ->
            binding.shops.adapter = ShopsAdapter(
                shops = shops,
                onShopClicked = viewModel::goShopDetails
            )
        }
        isParseFailed.observe(viewLifecycleOwner) { isParseFailed ->
            binding.parseFailedMessage.isVisible = isParseFailed
        }
        observeEvents()
    }

    private fun ShopsViewModel.observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            for (event in events) {
                when(event) {
                    is ShopEvent.ShowConnectionErrorMessage -> {
                        requireContext().showToast(R.string.connection_error)
                    }
                    is ShopEvent.ShowApiErrorMessage -> {
                        requireContext().showToast(getString(R.string.api_error, event.error.message))
                    }
                }
            }
        }
    }
}