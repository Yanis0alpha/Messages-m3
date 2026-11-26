package com.anindra.messages.data

import kotlin.math.roundToInt

/** One opacity scale shared by every backdrop-blurred Material surface. */
object TranslucentSurfaceOpacity {
    const val MIN_PERCENT = 30
    const val MAX_PERCENT = 95
    const val DEFAULT_PERCENT = 60
    const val STEP_PERCENT = 5
    const val SLIDER_STEPS = 12

    val defaultAlpha: Float = alpha(DEFAULT_PERCENT)

    fun normalize(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun fromSlider(value: Float): Int {
        val step = ((value - MIN_PERCENT) / STEP_PERCENT).roundToInt()
        return normalize(MIN_PERCENT + step * STEP_PERCENT)
    }

    fun alpha(percent: Int): Float = normalize(percent) / 100f
}
