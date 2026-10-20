package com.wood.exk.imagetovideo

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object SoundSynth {

    private const val SR = 44100

    fun generate(out: File, phases: List<WheelMotion.Phase>, durationSec: Float) {
        val total = (durationSec * SR).toInt()
        val buf = FloatArray(total)

        phases.forEach { ph ->
            val s = (ph.startTime * SR).toInt()
            val e = (ph.endTime * SR).toInt()
            var t = s
            val step = (SR / 18f).toInt()
            while (t < e) {
                val tl = (0.012f * SR).toInt()
                for (i in 0 until tl) {
                    if (t + i >= total) break
                    val env = exp(-8f * i / tl)
                    val freq = 1200f + Random.nextFloat() * 400f
                    buf[t + i] += (env * 0.18f * sin(2 * PI * freq * i / SR)).toFloat()
                }
                t += step
            }
            val lock = (ph.endTime * SR).toInt()
            val ll = (0.06f * SR).toInt()
            for (i in 0 until ll) {
                if (lock + i >= total) break
                val env = exp(-30f * i / ll)
                buf[lock + i] += (env * 0.5f * sin(2 * PI * 900f * i / SR)).toFloat()
            }
        }

        val finalAt = ((durationSec - 1.5f) * SR).toInt().coerceAtLeast(0)
        val finalLen = (0.35f * SR).toInt()
        for (i in 0 until finalLen) {
            if (finalAt + i >= total) break
            val env = exp(-10f * i / finalLen)
            buf[finalAt + i] += (env * 0.6f * sin(2 * PI * 220f * i / SR)).toFloat()
        }

        val header = 44
        val dataSize = total * 2
        val fileSize = header + dataSize - 8
        val bb = ByteBuffer.allocate(header + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(fileSize); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(SR); bb.putInt(SR * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(dataSize)
        for (s in buf) bb.putShort((s.coerceIn(-1f, 1f) * 32767).toInt().toShort())
        FileOutputStream(out).use { it.write(bb.array()) }
    }
}
