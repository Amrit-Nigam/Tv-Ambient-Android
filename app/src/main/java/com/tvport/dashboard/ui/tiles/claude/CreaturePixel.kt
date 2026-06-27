package com.tvport.dashboard.ui.tiles.claude

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Pixel creature ported 1:1 from the portfolio's animated favicon (creaturePixelCore.js +
 * useCreatureFavicon.js). A 20x20 grid; BODY = terracotta, EYE = near-black. Frames are built by
 * [patch]/[shift] exactly as the JS, then played on a hold-timed loop. Mapped to Claude states:
 * working -> breathe/blink/look alive; waiting -> surprise; done -> wink; idle/offline -> sleep.
 */
private const val B = 1 // BODY
private const val E = 2 // EYE
private val BODY_COLOR = Color(0xFFD97757)
private val EYE_COLOR = Color(0xFF1A1A1A)

private val CREATURE: Array<IntArray> = arrayOf(
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 0, 0),
    intArrayOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
    intArrayOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
    intArrayOf(0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
)

private fun shift(base: Array<IntArray>, dr: Int, dc: Int): Array<IntArray> {
    val out = Array(20) { IntArray(20) }
    for (r in 0 until 20) for (c in 0 until 20) {
        val nr = r + dr; val nc = c + dc
        if (nr in 0..19 && nc in 0..19) out[nr][nc] = base[r][c]
    }
    return out
}

private fun patch(base: Array<IntArray>, vararg ops: IntArray): Array<IntArray> {
    val out = Array(20) { base[it].copyOf() }
    for (o in ops) if (o[0] in 0..19 && o[1] in 0..19) out[o[0]][o[1]] = o[2]
    return out
}

private class Frame(val hold: Int, val grid: Array<IntArray>)

// ── look around ──
private val EYES_L = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(7,7,B),intArrayOf(6,13,B),intArrayOf(7,13,B),intArrayOf(6,6,E),intArrayOf(7,6,E),intArrayOf(6,12,E),intArrayOf(7,12,E))
private val HEAD_L_EYES = patch(shift(CREATURE,0,-1), intArrayOf(6,6,B),intArrayOf(7,6,B),intArrayOf(6,12,B),intArrayOf(7,12,B),intArrayOf(6,5,E),intArrayOf(7,5,E),intArrayOf(6,11,E),intArrayOf(7,11,E))
private val EYES_R = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(7,7,B),intArrayOf(6,13,B),intArrayOf(7,13,B),intArrayOf(6,8,E),intArrayOf(7,8,E),intArrayOf(6,14,E),intArrayOf(7,14,E))
private val HEAD_R_EYES = patch(shift(CREATURE,0,1), intArrayOf(6,8,B),intArrayOf(7,8,B),intArrayOf(6,14,B),intArrayOf(7,14,B),intArrayOf(6,9,E),intArrayOf(7,9,E),intArrayOf(6,15,E),intArrayOf(7,15,E))
private val EYES_UP = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(7,7,B),intArrayOf(6,13,B),intArrayOf(7,13,B),intArrayOf(5,7,E),intArrayOf(5,13,E))
private val HEAD_UP_EYES = patch(shift(CREATURE,-1,0), intArrayOf(5,7,B),intArrayOf(6,7,B),intArrayOf(5,13,B),intArrayOf(6,13,B),intArrayOf(4,7,E),intArrayOf(4,13,E))
private val CURIOUS = patch(CREATURE, intArrayOf(5,6,B),intArrayOf(5,14,B))
private val FRAMES_LOOK = listOf(
    Frame(800,CREATURE),Frame(200,EYES_L),Frame(500,HEAD_L_EYES),Frame(300,EYES_L),Frame(200,CREATURE),
    Frame(400,CURIOUS),Frame(400,CREATURE),Frame(200,EYES_R),Frame(500,HEAD_R_EYES),Frame(300,EYES_R),
    Frame(200,CREATURE),Frame(500,CREATURE),Frame(200,EYES_UP),Frame(400,HEAD_UP_EYES),Frame(300,EYES_UP),
    Frame(200,CREATURE),Frame(700,CREATURE),
)

// ── breathe ──
private val INHALE = shift(CREATURE,-1,0)
private val HALF_BLINK = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(6,13,B))
private val FULL_BLINK = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(7,7,B),intArrayOf(6,13,B),intArrayOf(7,13,B))
private val INHALE_BLINK = patch(INHALE, intArrayOf(5,7,B),intArrayOf(6,7,B),intArrayOf(5,13,B),intArrayOf(6,13,B))
private val DUST_L1 = patch(CREATURE, intArrayOf(2,1,B)); private val DUST_L2 = patch(CREATURE, intArrayOf(1,2,B))
private val DUST_R1 = patch(CREATURE, intArrayOf(3,18,B)); private val DUST_R2 = patch(CREATURE, intArrayOf(2,17,B))
private val FRAMES_BREATHE = listOf(
    Frame(500,CREATURE),Frame(200,DUST_L1),Frame(280,INHALE),Frame(80,INHALE_BLINK),Frame(360,INHALE),
    Frame(200,DUST_L2),Frame(320,CREATURE),Frame(80,HALF_BLINK),Frame(700,CREATURE),Frame(200,DUST_R1),
    Frame(300,INHALE),Frame(500,INHALE),Frame(200,DUST_R2),Frame(80,FULL_BLINK),Frame(80,HALF_BLINK),Frame(340,CREATURE),
)

// ── blink ──
private val GLANCE_DOWN = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(6,13,B),intArrayOf(8,7,E),intArrayOf(8,13,E))
private val EYEBROW = patch(CREATURE, intArrayOf(5,7,B),intArrayOf(5,13,B))
private val FRAMES_BLINK = listOf(
    Frame(2400,CREATURE),Frame(60,HALF_BLINK),Frame(100,FULL_BLINK),Frame(60,HALF_BLINK),Frame(80,CREATURE),
    Frame(220,GLANCE_DOWN),Frame(1600,CREATURE),Frame(60,HALF_BLINK),Frame(90,FULL_BLINK),Frame(60,HALF_BLINK),
    Frame(70,CREATURE),Frame(60,FULL_BLINK),Frame(100,HALF_BLINK),Frame(70,CREATURE),Frame(200,EYEBROW),Frame(900,CREATURE),
)

// ── sleep ──
private val SLEEP = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(7,7,B),intArrayOf(6,13,B),intArrayOf(7,13,B))
private val NOD_DOWN = shift(SLEEP,1,0); private val NOD_UP = shift(SLEEP,-1,0)
private val Z1 = patch(SLEEP, intArrayOf(3,15,B),intArrayOf(3,16,B),intArrayOf(4,15,B),intArrayOf(4,16,B))
private val Z2 = patch(SLEEP, intArrayOf(2,16,B),intArrayOf(2,17,B),intArrayOf(3,16,B),intArrayOf(3,17,B),intArrayOf(4,15,B))
private val Z3 = patch(SLEEP, intArrayOf(1,17,B),intArrayOf(1,18,B),intArrayOf(2,17,B),intArrayOf(2,18,B),intArrayOf(3,16,B))
private val Z4 = patch(SLEEP, intArrayOf(0,18,B),intArrayOf(1,18,B),intArrayOf(2,17,B))
private val Z5 = patch(SLEEP, intArrayOf(0,18,B),intArrayOf(0,19,B))
private val NOD_DOWN_Z = patch(NOD_DOWN, intArrayOf(4,15,B),intArrayOf(4,16,B))
private val NOD_UP_Z2 = patch(NOD_UP, intArrayOf(2,16,B),intArrayOf(2,17,B),intArrayOf(3,16,B))
private val FRAMES_SLEEP = listOf(
    Frame(600,SLEEP),Frame(400,NOD_DOWN),Frame(300,SLEEP),Frame(300,Z1),Frame(300,NOD_DOWN_Z),Frame(300,Z1),
    Frame(300,Z2),Frame(300,NOD_UP_Z2),Frame(300,Z3),Frame(300,NOD_DOWN),Frame(300,Z4),Frame(300,SLEEP),
    Frame(300,Z5),Frame(400,NOD_UP),Frame(700,SLEEP),
)

// ── wink (done / happy) ──
private val SQUINT_R = patch(CREATURE, intArrayOf(6,13,B))
private val WINK = patch(CREATURE, intArrayOf(6,13,B),intArrayOf(7,13,B))
private val TILT = shift(CREATURE,0,1)
private val TILT_WINK = patch(TILT, intArrayOf(6,14,B),intArrayOf(7,14,B))
private val SPARKLE1 = patch(TILT_WINK, intArrayOf(4,17,B),intArrayOf(5,18,B))
private val SPARKLE2 = patch(TILT_WINK, intArrayOf(3,18,B),intArrayOf(5,17,B))
private val SPARKLE3 = patch(TILT_WINK, intArrayOf(4,18,B))
private val TILT_SQUINT = patch(TILT, intArrayOf(6,14,B))
private val RETURN_SQUINT = patch(CREATURE, intArrayOf(6,13,B))
private val FRAMES_WINK = listOf(
    Frame(1200,CREATURE),Frame(100,SQUINT_R),Frame(120,WINK),Frame(150,TILT_WINK),Frame(120,SPARKLE1),
    Frame(100,SPARKLE2),Frame(100,SPARKLE3),Frame(400,TILT_WINK),Frame(100,TILT_SQUINT),Frame(100,RETURN_SQUINT),
    Frame(80,CREATURE),Frame(800,CREATURE),
)

// ── surprise (waiting / needs you) ──
private val ANTICIPATE = patch(CREATURE, intArrayOf(6,7,B),intArrayOf(6,13,B))
private val WIDE = patch(CREATURE, intArrayOf(6,6,E),intArrayOf(6,7,E),intArrayOf(6,8,E),intArrayOf(7,6,E),intArrayOf(7,7,E),intArrayOf(7,8,E),intArrayOf(6,12,E),intArrayOf(6,13,E),intArrayOf(6,14,E),intArrayOf(7,12,E),intArrayOf(7,13,E),intArrayOf(7,14,E))
private val RECOIL = patch(shift(CREATURE,-1,0), intArrayOf(5,6,E),intArrayOf(5,7,E),intArrayOf(5,8,E),intArrayOf(6,6,E),intArrayOf(6,7,E),intArrayOf(6,8,E),intArrayOf(5,12,E),intArrayOf(5,13,E),intArrayOf(5,14,E),intArrayOf(6,12,E),intArrayOf(6,13,E),intArrayOf(6,14,E))
private val SETTLE_HALF = patch(CREATURE, intArrayOf(6,6,E),intArrayOf(6,7,E),intArrayOf(6,13,E),intArrayOf(6,14,E),intArrayOf(7,7,E),intArrayOf(7,13,E))
private val FRAMES_SURPRISE = listOf(
    Frame(700,CREATURE),Frame(120,ANTICIPATE),Frame(900,WIDE),Frame(120,RECOIL),Frame(900,WIDE),
    Frame(180,SETTLE_HALF),Frame(500,WIDE),
)

// While busy, cycle through these alive animations — switched every 10s for variety.
private val ALIVE_SETS = listOf(FRAMES_BREATHE, FRAMES_BLINK, FRAMES_LOOK)
private const val ALIVE_SWITCH_MS = 10_000L

/** Renders the animated creature, picking the animation from the Claude state. */
@Composable
fun Creature(kind: ClaudeKind, busy: Boolean, modifier: Modifier = Modifier) {
    // Rotate the busy/alive animation every 10 seconds.
    var aliveIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(busy) {
        if (!busy) return@LaunchedEffect
        while (true) {
            delay(ALIVE_SWITCH_MS)
            aliveIdx = (aliveIdx + 1) % ALIVE_SETS.size
        }
    }

    val frames = remember(kind, busy, aliveIdx) {
        when {
            busy -> ALIVE_SETS[aliveIdx]
            kind == ClaudeKind.DONE -> FRAMES_WINK
            kind == ClaudeKind.WAITING || kind == ClaudeKind.PERMISSION -> FRAMES_SURPRISE
            else -> FRAMES_SLEEP // idle / offline
        }
    }
    var idx by remember(frames) { mutableIntStateOf(0) }
    LaunchedEffect(frames) {
        idx = 0
        while (true) {
            delay(frames[idx].hold.toLong())
            idx = (idx + 1) % frames.size
        }
    }
    val grid = frames[idx].grid
    Canvas(modifier) {
        val cs = min(size.width, size.height) / 20f
        for (r in 0 until 20) for (c in 0 until 20) {
            val v = grid[r][c]
            if (v == 0) continue
            drawRect(
                color = if (v == B) BODY_COLOR else EYE_COLOR,
                topLeft = Offset(c * cs, r * cs),
                size = Size(cs + 0.5f, cs + 0.5f),
            )
        }
    }
}
