package com.wood.exk.imagetovideo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object LayerSplitter {

    data class Layers(
        val background: File,
        val wheel: File,
        val wheelCenterX: Int,
        val wheelCenterY: Int,
        val wheelRadius: Int,
        val imageWidth: Int,
        val imageHeight: Int
    )

    fun split(imageFile: File, outDir: File): Layers {
        val original = BitmapFactory.decodeFile(imageFile.absolutePath)
        val mat = Mat()
        Utils.bitmapToMat(original, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        val circles = Mat()
        Imgproc.HoughCircles(
            gray, circles, Imgproc.HOUGH_GRADIENT,
            1.0, gray.rows().toDouble() / 4,
            100.0, 50.0,
            min(gray.rows(), gray.cols()) / 8,
            min(gray.rows(), gray.cols()) / 3
        )
        require(circles.cols() > 0) { "لم يتم كشف العجلة" }

        val c = circles.get(0, 0)!!
        val cx = c[0].toInt(); val cy = c[1].toInt(); val r = c[2].toInt()

        val mask = Mat.zeros(mat.size(), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cx.toDouble(), cy.toDouble()), r, Scalar(255.0), -1)
        val wheelMat = Mat(); mat.copyTo(wheelMat, mask)
        val invMask = Mat(); Core.bitwise_not(mask, invMask)
        val bgMat = Mat(); mat.copyTo(bgMat, invMask)

        val bgFile = File(outDir, "layer_bg.png")
        val wheelFile = File(outDir, "layer_wheel.png")
        saveMat(bgMat, bgFile); saveMat(wheelMat, wheelFile)

        return Layers(bgFile, wheelFile, cx, cy, r, original.width, original.height)
    }

    private fun saveMat(m: Mat, f: File) {
        val bmp = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(m, bmp)
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
