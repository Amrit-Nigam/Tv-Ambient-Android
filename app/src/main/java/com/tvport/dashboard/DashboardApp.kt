package com.tvport.dashboard

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt application root. Provides the WorkManager configuration so periodic refresh
 * workers can be @HiltWorker-injected. The default WorkManager initializer is removed
 * in the manifest so this on-demand config is the single source.
 */
@HiltAndroidApp
class DashboardApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
