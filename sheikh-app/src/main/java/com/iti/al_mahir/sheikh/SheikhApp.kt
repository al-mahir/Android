package com.iti.al_mahir.sheikh

import android.app.Application
import com.iti.data.di.almahirDataModule
import com.iti.data.sheikh.auth.di.sheikhAuthDataModule
import com.iti.data.sheikh.di.almahirSheikhDataModule
import com.iti.domain.auth.di.authDomainModule
import com.iti.presentation.auth.di.authPresentationModule
import com.iti.presentation.di.presentationModule
import com.iti.sheikh.presentation.di.sheikhPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SheikhApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SheikhApp)
            modules(
                almahirDataModule,
                presentationModule,
                sheikhPresentationModule,
                sheikhAuthDataModule,
                almahirSheikhDataModule,
                authDomainModule,
                authPresentationModule,
            )
        }
    }
}
