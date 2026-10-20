package com.wood.exk.imagetovideo

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.util.Locale

object WheelRenderer {

    fun render(
        layers: LayerSplitter.Layers,
        outputFile: File,
        fps: Int = 60,
        durationSec: Float = 18f
    ): Boolean {
        val phases = WheelMotion.defaultSequence()
        val soundFile = File(outputFile.parentFile, "mech.wav")
        SoundSynth.generate(soundFile, phases, durationSec)

        val wheelSize = layers.wheelRadius * 2
        val offX = layers.wheelCenterX - layers.wheelRadius
        val offY = layers.wheelCenterY - layers.wheelRadius
        val w = layers.imageWidth
        val h = layers.imageHeight

        val angleExprs = mutableListOf<String>()
        phases.forEach { ph ->
            val sf = (ph.startTime * fps).toInt()
            val ef = (ph.endTime * fps).toInt()
            val sr = Math.toRadians(ph.startAngle.toDouble())
            val er = Math.toRadians(ph.endAngle.toDouble())
            val f = "((n-$sf)/max(1,$ef-$sf))"
            val eased = "0.5*(1-cos(PI*clamp($f,0,1)))"
            angleExprs += String.format(Locale.US,
                "if(between(n,$sf,$ef),%f+%f*%s,0)", sr, (er - sr), eased)
        }
        val angleExpr = "(" + angleExprs.joinToString("+") + ")"

        val filter = listOf(
            "[0:v]scale=$w:$h,format=rgba[bg]",
            "[1:v]format=rgba,scale=$wheelSize:$wheelSize,rotate='$angleExpr':c=none:ow=$wheelSize:oh=$wheelSize[wheel]",
            "[bg][wheel]overlay=$offX:$offY[out]",
            "[out]format=yuv420p[final]"
        ).joinToString(";")

        val args = mutableListOf<String>()
        args += listOf("-loop", "1", "-t", durationSec.toString(), "-i", layers.background.absolutePath)
        args += listOf("-loop", "1", "-t", durationSec.toString(), "-i", layers.wheel.absolutePath)
        args += listOf("-i", soundFile.absolutePath)
        args += listOf(
            "-filter_complex", filter,
            "-map", "[final]",
            "-map", "2:a",
            "-c:v", "libx264",
            "-preset", "slow",
            "-crf", "18",
            "-pix_fmt", "yuv420p",
            "-r", fps.toString(),
            "-c:a", "aac",
            "-shortest",
            "-movflags", "+faststart",
            "-y",
            outputFile.absolutePath
        )

        val session = FFmpegKit.executeWithArguments(args.toTypedArray())
        return ReturnCode.isSuccess(session.returnCode)
    }
}
