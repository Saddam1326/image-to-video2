package com.wood.exk.imagetovideo

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.util.Locale

object RenderingEngine {

    fun renderProject(
        ctx: Context,
        input: ProjectInput,
        workDir: File,
        output: File
    ): Boolean {
        val plans = PromptCompiler.compile(input)
        val imageFiles = input.images.mapIndexed { i, img ->
            val f = File(workDir, "img_$i.jpg")
            ctx.contentResolver.openInputStream(img.uri)?.use { inp ->
                f.outputStream().use { out -> inp.copyTo(out) }
            }
            f
        }

        if (imageFiles.size == 1 && plans[0].staticCanvas) {
            val layers = LayerSplitter.split(imageFiles[0], workDir)
            return WheelRenderer.render(layers, output)
        }

        return renderDynamic(imageFiles, plans, input, output)
    }

    private fun renderDynamic(
        images: List<File>,
        plans: List<PromptCompiler.ImagePlan>,
        input: ProjectInput,
        output: File
    ): Boolean {
        val w = input.ratio.w
        val h = input.ratio.h
        val args = mutableListOf<String>()

        images.forEach { img ->
            args += listOf("-loop", "1", "-t", "8", "-i", img.absolutePath)
        }

        val filters = mutableListOf<String>()
        plans.forEachIndexed { i, plan ->
            val dur = input.images[i].durationSec
            val frames = (dur * input.fps).toInt()
            val zEnd = plan.strength
            val (fx, fy) = focalExpr(plan.focalPoint)

            val zExpr = when (plan.motion) {
                PromptCompiler.Motion.ZOOM_IN, PromptCompiler.Motion.PUSH_FAST ->
                    "min(zoom+${(zEnd - 1f) / frames},$zEnd)"
                PromptCompiler.Motion.ZOOM_OUT ->
                    "if(lte(zoom,1.0),$zEnd,max(1.001,zoom-${(zEnd - 1f) / frames}))"
                PromptCompiler.Motion.ORBIT ->
                    "1.2+0.05*sin(2*PI*on/$frames)"
                else -> "min(zoom+0.0015,1.5)"
            }

            val zp = "zoompan=z='$zExpr':x='$fx':y='$fy':d=$frames:s=${w}x${h}:fps=${input.fps}"
            val color = if (plan.colorGrade.ff.isNotBlank()) ",${plan.colorGrade.ff}" else ""
            val vig = if (plan.addVignette) ",vignette=PI/4" else ""
            val grain = if (plan.addGrain) ",noise=alls=15:allf=t+u" else ""
            val bloom = if (plan.addBloom) ",gblur=sigma=8" else ""

            filters += "[$i:v]$zp$color$vig$grain$bloom,setsar=1[v$i]"
        }

        var lastLabel = "[v0]"
        var offset = 0.0
        for (i in 1 until images.size) {
            offset += input.images[i - 1].durationSec - 0.8
            val outLabel = if (i == images.size - 1) "[vout]" else "[x$i]"
            val tr = plans[i].transition.ff
            filters += "$lastLabel[v$i]xfade=transition=$tr:duration=0.8:offset=${String.format(Locale.US, "%.2f", offset)}$outLabel"
            lastLabel = outLabel
        }

        val totalDur = input.images.sumOf { it.durationSec.toDouble() } - (images.size - 1) * 0.8
        val filterStr = filters.joinToString(";") + ";$lastLabel,format=yuv420p[final]"

        args += listOf(
            "-filter_complex", filterStr,
            "-map", "[final]",
            "-t", String.format(Locale.US, "%.2f", totalDur),
            "-c:v", "libx264",
            "-preset", "slow",
            "-crf", "18",
            "-pix_fmt", "yuv420p",
            "-r", input.fps.toString(),
            "-movflags", "+faststart",
            "-y",
            output.absolutePath
        )

        val session = FFmpegKit.executeWithArguments(args.toTypedArray())
        return ReturnCode.isSuccess(session.returnCode)
    }

    private fun focalExpr(fp: PromptCompiler.FocalPoint): Pair<String, String> = when (fp) {
        PromptCompiler.FocalPoint.TOP -> "iw/2-(iw/zoom/2)" to "0"
        PromptCompiler.FocalPoint.BOTTOM -> "iw/2-(iw/zoom/2)" to "ih-(ih/zoom)"
        PromptCompiler.FocalPoint.LEFT -> "0" to "ih/2-(ih/zoom/2)"
        PromptCompiler.FocalPoint.RIGHT -> "iw-(iw/zoom)" to "ih/2-(ih/zoom/2)"
        else -> "iw/2-(iw/zoom/2)" to "ih/2-(ih/zoom/2)"
    }
}
