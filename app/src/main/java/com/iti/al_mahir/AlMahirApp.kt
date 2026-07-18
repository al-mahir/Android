package com.iti.al_mahir

import android.app.Application
import com.example.mushaf.data.di.mushafDataModule
import com.example.mushaf.presentation.di.mushafPresentationModule
import com.iti.data.auth.di.authDataModule
import com.iti.domain.auth.di.authDomainModule
import com.iti.presentation.auth.di.authPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AlMahirApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AlMahirApp)
            modules(
                mushafDataModule,
                mushafPresentationModule,
                authDataModule,
                authDomainModule,
                authPresentationModule,
            )
        }
    }
}
