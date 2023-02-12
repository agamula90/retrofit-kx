package com.github.retrofitx.android.settings

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.retrofitx.android.R
import com.github.retrofitx.android.databinding.SettingsBinding
import com.github.retrofitx.android.utils.hideKeyboard
import com.github.retrofitx.android.utils.setTextIfChanged
import com.github.retrofitx.android.utils.showToast
import com.github.retrofitx.android.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment: Fragment(R.layout.settings) {
    private val viewModel by viewModels<SettingsViewModel>()
    private val binding by viewBinding(SettingsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setup()
        viewModel.observeChanges()
    }

    private fun SettingsBinding.setup() {
        baseUrl.doAfterTextChanged { viewModel.setBaseUrl(it.toString()) }
        confirmBaseUrlChange.setOnClickListener {
            viewModel.confirmBaseUrlChange()
            it.context.apply {
                hideKeyboard()
                showToast(R.string.base_url_successfully_changed)
            }
        }
    }

    private fun SettingsViewModel.observeChanges() {
        baseUrl.observe(viewLifecycleOwner) {
            binding.baseUrl.setTextIfChanged(it)
        }
        isBaseUrlChanged.observe(viewLifecycleOwner) { isBaseUrlChanged ->
            binding.confirmBaseUrlChange.isEnabled = isBaseUrlChanged
        }
    }
}