package com.github.retrofitx.android

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class NavEvent {
    class Forward(val actionId: Int, val args: Bundle?): NavEvent()
    class Backward(val destinationId: Int?, val inclusive: Boolean): NavEvent()
}

class StateHandleRequest(
    val backStackDestinationId: Int?,
    val stateHandleCallback: (SavedStateHandle) -> Unit
)

@Singleton
class NavigationDispatcher @Inject constructor() {
    val events = Channel<NavEvent>()
    val stateHandleRequests = Channel<StateHandleRequest>()

    fun navigate(@IdRes actionId: Int, args: Bundle? = null) {
        events.trySend(NavEvent.Forward(actionId, args))
    }

    fun navigateBack(@IdRes destinationId: Int? = null, inclusive: Boolean = false) {
        events.trySend(NavEvent.Backward(destinationId, inclusive))
    }

    suspend fun <T> setNavigationResult(
        @IdRes backStackDestinationId: Int,
        navigationKey: String,
        value: T
    ) = suspendCoroutine { continuation ->
        stateHandleRequests.trySend(
            StateHandleRequest(
                backStackDestinationId = backStackDestinationId,
                stateHandleCallback = {
                    it[navigationKey] = value
                    continuation.resume(Unit)
                }
            )
        )
    }

    fun <T> observeNavigationResult(
        navigationKey: String,
        initialValue: T,
        coroutineScope: CoroutineScope,
        callback: suspend (T) -> Unit
    ) {
        stateHandleRequests.trySend(
            StateHandleRequest(
                backStackDestinationId = null,
                stateHandleCallback = { stateHandle ->
                    coroutineScope.launch {
                        stateHandle.getStateFlow(navigationKey, initialValue).collect {
                            callback(it)
                            stateHandle[navigationKey] = initialValue
                        }
                    }
                }
            )
        )
    }
}