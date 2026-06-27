package com.tvport.dashboard.core

/**
 * Shared contract every dashboard tile exposes via a StateFlow<TileState<T>>.
 *
 * The whole resilience model of the app rests on this: a tile is NEVER allowed to throw
 * up to the dashboard. It moves between these four states and the UI renders each one.
 *
 *  - [Loading]  : first fetch in flight, nothing to show yet.
 *  - [Content]  : real data from the real API/source. The happy path.
 *  - [Idle]     : a deliberate, tasteful "nothing right now" state (e.g. nothing playing,
 *                 calendar empty). NOT an error — a designed quiet state.
 *  - [Fallback] : the real source is unavailable (missing secret, network down, API error).
 *                 Carries an optional [data] payload so a tile can show a *labeled* static
 *                 fallback (e.g. the FIFA static match) instead of blanking.
 *
 * Per BUILD SPEC §11/§16: every tile fails independently and gracefully. One dead API
 * must never blank the screen or crash the app.
 */
sealed interface TileState<out T> {
    data object Loading : TileState<Nothing>

    data class Content<T>(val data: T) : TileState<T>

    /** Designed empty state. [message] is shown to the viewer; not an error. */
    data class Idle(val message: String) : TileState<Nothing>

    /**
     * Source unavailable. [reason] is for logs/devs; [data] is an optional labeled
     * fallback payload to keep the tile looking alive.
     */
    data class Fallback<T>(val reason: String, val data: T? = null) : TileState<T>
}
