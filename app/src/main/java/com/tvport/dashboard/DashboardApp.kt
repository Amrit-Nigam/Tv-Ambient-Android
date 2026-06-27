package com.tvport.dashboard

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt application root. Provides the WorkManager configuration so periodic refresh
 * workers can be @HiltWorker-injected. The default WorkManager initializer is removed
 * in the manifest so this on-demand config is the single source.
 *
 * Also supplies the app-wide Coil [ImageLoader] with the platform-appropriate animated-GIF
 * decoder so the Claude creature's clawd GIFs play (ImageDecoder on API 28+, GifDecoder below).
 */
@HiltAndroidApp
class DashboardApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
}
