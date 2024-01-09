package io.github.retrofitx.android

import android.app.Application
import com.github.venom.Venom
import com.github.venom.service.NotificationConfig
import io.github.retrofitx.android.inject.AnnotationBasedModule
import io.github.retrofitx.android.inject.remoteModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.*

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

        startKoin {
            androidContext(this@RetrofitKxApp)
            androidLogger()
            modules(remoteModule, AnnotationBasedModule.module)
        }
    }
}