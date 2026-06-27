package com.tvport.dashboard.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits [Unit] every [periodMs], aligned to the wall clock for the 1s case so the clock
 * ticks on the second boundary. A suspending `delay` loop — not a busy loop (BUILD SPEC §6).
 */
fun tickerFlow(periodMs: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        if (periodMs == 1000L) {
            // align to next whole second to avoid drift
            val now = System.currentTimeMillis()
            delay(1000L - (now % 1000L))
        } else {
            delay(periodMs)
        }
    }
}
