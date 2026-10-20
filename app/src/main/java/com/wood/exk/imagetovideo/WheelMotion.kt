package com.wood.exk.imagetovideo

import kotlin.math.PI
import kotlin.math.cos

object WheelMotion {

    data class Phase(
        val direction: Int,
        val startAngle: Float,
        val endAngle: Float,
        val startTime: Float,
        val endTime: Float
    )

    fun defaultSequence(): List<Phase> = listOf(
        Phase(+1, 0f, 360f, 0f, 3.5f),
        Phase(-1, 360f, 80f, 4.0f, 7.5f),
        Phase(+1, 80f, 280f, 8.0f, 11.5f),
        Phase(-1, 280f, 60f, 12.0f, 16.0f)
    )

    fun easeInOut(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return 0.5f * (1f - cos(PI * c).toFloat())
    }

    fun angleAt(phases: List<Phase>, timeSec: Float): Float {
        for (p in phases) {
            if (timeSec in p.startTime..p.endTime) {
                val t = (timeSec - p.startTime) / (p.endTime - p.startTime)
                val e = easeInOut(t)
                return p.startAngle + (p.endAngle - p.startAngle) * e
            }
        }
        return phases.lastOrNull()?.endAngle ?: 0f
    }
}
