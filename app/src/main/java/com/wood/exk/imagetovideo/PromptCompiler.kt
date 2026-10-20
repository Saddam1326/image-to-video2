package com.wood.exk.imagetovideo

object PromptCompiler {

    data class ImagePlan(
        val motion: Motion,
        val transition: Transition,
        val colorGrade: Grade,
        val strength: Float,
        val staticCanvas: Boolean,
        val addVignette: Boolean,
        val addGrain: Boolean,
        val addBloom: Boolean,
        val focalPoint: FocalPoint
    )

    enum class Motion { ZOOM_IN, ZOOM_OUT, PAN_LEFT, PAN_RIGHT, ORBIT, KEN_BURNS, PUSH_FAST, STATIC }
    enum class Transition(val ff: String) {
        FADE("fade"), DISSOLVE("dissolve"), SLIDE_L("slideleft"),
        ZOOM_BLUR("zoomin"), CIRCLE("circleopen"), WIPE("wipeleft")
    }
    enum class Grade(val ff: String) {
        NONE(""),
        NEON("colorbalance=rs=0.3:gs=-0.1:bs=0.4,eq=saturation=1.4"),
        WARM("colorbalance=rs=0.15:bs=-0.1,eq=saturation=1.2"),
        COLD("colorbalance=rs=-0.15:bs=0.2,eq=saturation=1.1"),
        NOIR("hue=s=0,eq=contrast=1.3"),
        EPIC("curves=preset=strong_contrast,eq=saturation=1.3:contrast=1.2")
    }
    enum class FocalPoint { CENTER, TOP, BOTTOM, LEFT, RIGHT }

    fun compile(input: ProjectInput): List<ImagePlan> =
        input.images.map {
            buildImagePlan("${input.globalPrompt} ${it.description} ${it.motionHint}".lowercase())
        }

    private fun buildImagePlan(p: String): ImagePlan {
        val static = p.containsAny("ثابت", "ثبّت", "لا تحرك", "لا تعيد", "static", "fixed")
        val motion = when {
            p.containsAny("دوران", "orbit", "spin") -> Motion.ORBIT
            p.containsAny("سريع", "انفجاري", "fast") -> Motion.PUSH_FAST
            p.containsAny("اقتراب", "zoom in", "دفع") -> Motion.ZOOM_IN
            p.containsAny("ابتعاد", "zoom out", "سحب") -> Motion.ZOOM_OUT
            p.containsAny("يمين", "right") -> Motion.PAN_RIGHT
            p.containsAny("يسار", "left") -> Motion.PAN_LEFT
            static -> Motion.STATIC
            else -> Motion.KEN_BURNS
        }
        val trans = when {
            p.containsAny("ضباب", "fog", "dissolve") -> Transition.DISSOLVE
            p.containsAny("نيون", "neon", "glow") -> Transition.ZOOM_BLUR
            p.containsAny("انزلاق", "slide") -> Transition.SLIDE_L
            p.containsAny("دائري", "circle") -> Transition.CIRCLE
            p.containsAny("مسح", "wipe") -> Transition.WIPE
            else -> Transition.FADE
        }
        val grade = when {
            p.containsAny("نيون", "بنفسجي", "cyberpunk") -> Grade.NEON
            p.containsAny("دافئ", "غروب", "warm") -> Grade.WARM
            p.containsAny("بارد", "ليل", "cold", "night") -> Grade.COLD
            p.containsAny("أسود", "أبيض", "noir") -> Grade.NOIR
            p.containsAny("ضخم", "ملحمي", "epic") -> Grade.EPIC
            else -> Grade.NONE
        }
        val strength = when {
            p.containsAny("قوي", "ضخم", "epic") -> 2.2f
            p.containsAny("خفيف", "هادئ", "subtle") -> 1.15f
            else -> 1.5f
        }
        val focal = when {
            p.containsAny("أعلى", "top") -> FocalPoint.TOP
            p.containsAny("أسفل", "bottom") -> FocalPoint.BOTTOM
            p.containsAny("يسار", "left") -> FocalPoint.LEFT
            p.containsAny("يمين", "right") -> FocalPoint.RIGHT
            else -> FocalPoint.CENTER
        }
        return ImagePlan(
            motion, trans, grade, strength, static,
            p.containsAny("سينمائي", "vignette", "epic", "ضخم"),
            p.containsAny("فيلم", "grain", "قديم"),
            p.containsAny("نيون", "glow", "توهج"),
            focal
        )
    }

    private fun String.containsAny(vararg words: String) = words.any { contains(it) }
}
