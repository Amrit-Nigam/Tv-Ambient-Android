package com.tvport.dashboard.ui.visualizer

import android.media.audiofx.Visualizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Wraps [android.media.audiofx.Visualizer] on the global output mix (audio session 0).
 *
 * In practice this only yields data for audio the app is allowed to read; protected
 * streams (e.g. Spotify) generally return silence or throw. EVERYTHING here is wrapped
 * in try/catch and falls back silently — there is never a runtime-permission dialog
 * (awkward on a TV) and never a crash. When no readable audio exists, [level] simply
 * stays at 0 and the procedural field animates on its own.
 */

/** Number of smoothed magnitude buckets exposed to the drawing layer. */
const val BAND_COUNT = 24

/**
 * Shared, reusable state for audio reactivity. Mutated in place from the capture
 * callback (no per-frame allocation) and read by the Canvas each frame.
 */
class AudioState {
    /** Smoothed per-band magnitudes in 0f..1f. Read-only intent for consumers. */
    val bands = FloatArray(BAND_COUNT)

    /** Overall smoothed loudness in 0f..1f, handy for global pulse. */
    private val _level = mutableStateOf(0f)
    val level: State<Float> get() = _level

    /** True once we have actually received non-trivial audio data. */
    private val _active = mutableStateOf(false)
    val active: State<Boolean> get() = _active

    /** Scratch buffer reused every callback so the hot path allocates nothing. */
    private val raw = FloatArray(BAND_COUNT)

    fun submitWaveform(data: ByteArray) {
        // Down-sample the waveform into BAND_COUNT RMS buckets.
        val n = data.size
        if (n == 0) return
        val per = (n / BAND_COUNT).coerceAtLeast(1)
        var loud = 0f
        for (b in 0 until BAND_COUNT) {
            var sum = 0f
            var count = 0
            val start = b * per
            var i = start
            val end = min(start + per, n)
            while (i < end) {
                // Waveform bytes are signed-ish 0..255 centered ~128.
                val v = (data[i].toInt() and 0xFF) - 128
                sum += (v * v).toFloat()
                count++
                i++
            }
            val rms = if (count > 0) sqrt(sum / count) / 128f else 0f
            raw[b] = rms.coerceIn(0f, 1f)
            loud += raw[b]
        }
        commit(loud / BAND_COUNT)
    }

    fun submitFft(data: ByteArray) {
        // FFT layout: data[0]=DC, data[1]=Nyquist, then (real, imag) pairs.
        val n = data.size
        if (n < 2) return
        val pairs = (n / 2)
        val per = (pairs / BAND_COUNT).coerceAtLeast(1)
        var loud = 0f
        for (b in 0 until BAND_COUNT) {
            var sum = 0f
            var count = 0
            val start = b * per
            var p = start
            val end = min(start + per, pairs)
            while (p < end) {
                val re = data[2 * p].toFloat()
                val im = data[2 * p + 1].toFloat()
                sum += sqrt(re * re + im * im)
                count++
                p++
            }
            // Normalize: magnitudes are bounded by ~128*sqrt(2); emphasize lows lightly.
            val mag = if (count > 0) (sum / count) / 180f else 0f
            raw[b] = mag.coerceIn(0f, 1f)
            loud += raw[b]
        }
        commit(loud / BAND_COUNT)
    }

    private fun commit(loudness: Float) {
        // Exponential smoothing toward the new frame; fast attack, slow release feel.
        var peak = 0f
        for (b in 0 until BAND_COUNT) {
            val target = raw[b]
            val cur = bands[b]
            val a = if (target > cur) 0.45f else 0.12f
            val next = cur + (target - cur) * a
            bands[b] = next
            if (next > peak) peak = next
        }
        val lv = _level.value
        val la = if (loudness > lv) 0.4f else 0.1f
        _level.value = (lv + (loudness - lv) * la).coerceIn(0f, 1f)
        if (!_active.value && (peak > 0.02f || abs(loudness) > 0.02f)) {
            _active.value = true
        }
    }

    fun markInactive() {
        _active.value = false
    }
}

/**
 * Creates an [AudioState] and manages a [Visualizer] tied to the composition lifecycle.
 * Returns state that is safe to read every frame. On any failure it returns a quiet
 * [AudioState] (active=false) and the caller falls back to the procedural field.
 */
@Composable
fun rememberAudioState(): AudioState {
    val state = remember { AudioState() }

    DisposableEffect(Unit) {
        var visualizer: Visualizer? = null
        try {
            visualizer = Visualizer(0).apply {
                // Smallest capture size for low CPU; both wave + FFT.
                val range = Visualizer.getCaptureSizeRange()
                captureSize = range[0]
                val rate = Visualizer.getMaxCaptureRate() / 2

                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (waveform != null) {
                                try {
                                    state.submitWaveform(waveform)
                                } catch (_: Throwable) { /* never crash the UI thread */ }
                            }
                        }

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (fft != null) {
                                try {
                                    state.submitFft(fft)
                                } catch (_: Throwable) { }
                            }
                        }
                    },
                    rate,
                    true,  // waveform
                    true,  // fft
                )
                enabled = true
            }
        } catch (_: SecurityException) {
            visualizer = safeRelease(visualizer)
        } catch (_: UnsupportedOperationException) {
            visualizer = safeRelease(visualizer)
        } catch (_: RuntimeException) {
            // Visualizer ctor throws RuntimeException("Cannot initialize Visualizer engine")
            visualizer = safeRelease(visualizer)
        } catch (_: Throwable) {
            visualizer = safeRelease(visualizer)
        }

        onDispose {
            state.markInactive()
            safeRelease(visualizer)
        }
    }

    return state
}

private fun safeRelease(v: Visualizer?): Visualizer? {
    try {
        v?.enabled = false
    } catch (_: Throwable) { }
    try {
        v?.release()
    } catch (_: Throwable) { }
    return null
}
