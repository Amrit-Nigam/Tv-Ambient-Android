package com.tvport.dashboard.ui.tiles.claude

import kotlinx.serialization.Serializable

/**
 * Mirrors ~/.claude/statusbar/state.json served by serve.py.
 * state ∈ thinking | tool | done | waiting | permission | idle.
 */
@Serializable
data class ClaudeStateDto(
    val state: String = "idle",
    val label: String = "",
    val tool: String = "",
    val project: String = "",
    val startedAt: Long = 0,
    val ts: Long = 0,
)

/** What the tile renders. [busy] drives the pulsing accent; [kind] picks the color. */
data class ClaudeUi(
    val kind: ClaudeKind,
    val label: String,
    val project: String,
    val startedAt: Long, // epoch seconds, 0 if not timing
    val busy: Boolean,
)

enum class ClaudeKind { THINKING, TOOL, WAITING, PERMISSION, DONE, IDLE }

fun ClaudeStateDto.toUi(): ClaudeUi {
    val kind = when (state.lowercase()) {
        "thinking" -> ClaudeKind.THINKING
        "tool" -> ClaudeKind.TOOL
        "waiting" -> ClaudeKind.WAITING
        "permission" -> ClaudeKind.PERMISSION
        "done" -> ClaudeKind.DONE
        else -> ClaudeKind.IDLE
    }
    val text = label.ifBlank {
        when (kind) {
            ClaudeKind.THINKING -> "Thinking…"
            ClaudeKind.TOOL -> "Working…"
            ClaudeKind.WAITING -> "Waiting for you"
            ClaudeKind.PERMISSION -> "Awaiting permission"
            ClaudeKind.DONE -> "Done"
            ClaudeKind.IDLE -> "Idle"
        }
    }
    return ClaudeUi(
        kind = kind,
        label = text,
        project = project,
        startedAt = startedAt,
        busy = kind == ClaudeKind.THINKING || kind == ClaudeKind.TOOL,
    )
}
