package io.github.retrofitx.android

import android.app.Application
import com.github.venom.Venom
import com.github.venom.service.NotificationConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RetrofitKxApp: Application() {

    override fun onCreate() {
        super.onCreate()
        val config = NotificationConfig.Builder(this)
            .buttonCancel(com.github.venom.R.string.venom_notification_button_cancel)
            .buttonKill(getString(com.github.venom.R.string.venom_notification_button_kill))
            .build()

        Venom.createInstance(this).apply {
            initialize(config)
            start()
        }
    }
}