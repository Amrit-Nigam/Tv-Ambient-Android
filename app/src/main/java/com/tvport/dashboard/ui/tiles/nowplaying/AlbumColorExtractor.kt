package com.tvport.dashboard.ui.tiles.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.tvport.dashboard.core.AlbumScheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads an album-art bitmap via Coil and extracts a vivid accent scheme with androidx.palette.
 * Returns null on any failure (the page then keeps its previous/default scheme). All swatch hues
 * are nudged toward higher saturation/brightness so dark covers still yield a lively page accent.
 */
@Singleton
class AlbumColorExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun extract(url: String?): AlbumScheme? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false) // Palette needs a readable (software) bitmap
                    .size(128)
                    .build()
                val drawable = context.imageLoader.execute(request).drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                if (bitmap == null) {
                    android.util.Log.w("AlbumColor", "No bitmap (drawable=${drawable?.javaClass?.simpleName}) for $url")
                    return@withContext null
                }
                schemeFrom(bitmap).also {
                    android.util.Log.d("AlbumColor", "scheme accent=#${Integer.toHexString(it.accent)} for $url")
                }
            } catch (e: Exception) {
                android.util.Log.w("AlbumColor", "extract failed: ${e.message}", e)
                null
            }
        }
    }

    private fun schemeFrom(bitmap: Bitmap): AlbumScheme {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        // Prefer lively swatches; fall back through the chain so we always get something.
        val accentSrc = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
        val accent2Src = palette.lightVibrantSwatch
            ?: palette.lightMutedSwatch
            ?: accentSrc
        val accent = vivid(accentSrc?.rgb ?: DEFAULT_ACCENT)
        val accent2 = vivid(accent2Src?.rgb ?: accent, lighten = true)
        return AlbumScheme(accent = accent, accent2 = accent2, glow = accent)
    }

    /** Push a color toward a punchy saturation/brightness so it reads well on a dark UI. */
    private fun vivid(argb: Int, lighten: Boolean = false): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[1] = (hsv[1] * 1.25f).coerceIn(0.45f, 1f)       // saturation floor so greys gain color
        hsv[2] = (if (lighten) hsv[2] * 1.18f else hsv[2]).coerceIn(0.62f, 0.96f) // bright but not white
        return android.graphics.Color.HSVToColor(hsv)
    }

    private companion object {
        const val DEFAULT_ACCENT = 0xFF5BE0C4.toInt() // teal fallback (matches base theme)
    }
}
