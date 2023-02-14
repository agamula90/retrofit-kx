package io.github.retrofitx.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.retrofitx.android.BuildConfig
import io.github.retrofitx.android.R
import io.github.retrofitx.android.databinding.ActivityMainBinding
import io.github.retrofitx.android.simple.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.setupHints()
        binding.setupNavigation()
    }

    private fun ActivityMainBinding.setupHints() {
        staticBaseUrl.text = getString(R.string.static_base_url_text, BuildConfig.BASE_URL)
        lifecycleScope.launch {
            dataStoreManager.getBaseUrl().collect {
                dynamicBaseUrl.text = getString(R.string.dynamic_base_url_text, it)
            }
        }
    }

    private fun ActivityMainBinding.setupNavigation() {
        navHost.getFragment<NavHostFragment>().navController.apply {
            graph = navInflater.inflate(R.navigation.main_graph).apply {
                setStartDestination(R.id.shops)
            }
            setupBottomNavigation(bottomNavigation)
            observeNavigationEvents()
            observeStateHandleRequests()
        }
    }

    private fun NavController.observeNavigationEvents() = lifecycleScope.launch {
        for (event in navigationDispatcher.events) {
            when (event) {
                is NavEvent.Forward -> navigate(event.actionId, event.args)
                is NavEvent.Backward -> {
                    if (event.destinationId == null) {
                        popBackStack()
                    } else {
                        popBackStack(event.destinationId, event.inclusive)
                    }
                }
            }
        }
    }

    private fun NavController.observeStateHandleRequests() = lifecycleScope.launch {
        for (stateHandleRequest in navigationDispatcher.stateHandleRequests) {
            if (stateHandleRequest.backStackDestinationId == null) {
                stateHandleRequest.stateHandleCallback(currentBackStackEntry!!.savedStateHandle)
            } else {
                stateHandleRequest.stateHandleCallback(
                    getBackStackEntry(stateHandleRequest.backStackDestinationId).savedStateHandle
                )
            }
        }
    }

    private fun NavController.setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener { item ->
            navigate(
                resId = item.itemId,
                args = null,
                navOptions = navOptions {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(graph.findStartDestination().id) {
                        saveState = true
                    }
                }
            )
            true
        }
    }
}