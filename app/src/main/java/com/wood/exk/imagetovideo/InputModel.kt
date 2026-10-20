package com.wood.exk.imagetovideo

import android.net.Uri

data class ImageInput(
    val uri: Uri,
    val description: String = "",
    val motionHint: String = "",
    val durationSec: Float = 4f
)

data class ProjectInput(
    val images: List<ImageInput>,
    val globalPrompt: String = "",
    val ratio: Ratio = Ratio.SQUARE,
    val quality: Quality = Quality.UHD4K,
    val fps: Int = 60,
    val addAudio: Boolean = true
) {
    enum class Ratio(val w: Int, val h: Int) {
        SQUARE(2160, 2160),
        VERTICAL(2160, 3840),
        HORIZONTAL(3840, 2160)
    }
    enum class Quality(val longSide: Int) {
        FHD(1080), QHD(1440), UHD4K(2160)
    }
}
