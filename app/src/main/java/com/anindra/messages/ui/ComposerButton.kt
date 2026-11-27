package com.anindra.messages.ui

/** What the composer's single circle is currently for. */
internal enum class ComposerCircle { Record, Send }

/** The glyph drawn inside that circle. */
internal enum class ComposerGlyph { Wave, Send }

/**
 * The composer button's state, decided once.
 *
 * The idle voice pill and its recording arch keep the Material You accent;
 * cancellation owns the red state on the separate discard arch. Deciding the
 * mode, glyph and fill here keeps drawing and gesture state in agreement.
 */
internal object ComposerButton {

    fun modeFor(micMode: Boolean): ComposerCircle =
        if (micMode) ComposerCircle.Record else ComposerCircle.Send

    fun glyphFor(mode: ComposerCircle): ComposerGlyph =
        if (mode == ComposerCircle.Record) ComposerGlyph.Wave else ComposerGlyph.Send

    /** The voice/send control stays accented; only the discard target turns red. */
    @Suppress("UNUSED_PARAMETER")
    fun usesAccentFill(mode: ComposerCircle, recording: Boolean): Boolean = true
}

/** How the glyph swaps: a quarter turn with a small pinch, then a spring back. */
internal object ComposerSwap {
    const val TURN_DEGREES = 90f
    const val SCALE = 0.5f

    /** A little overshoot on the settle, as WhatsApp's icon swap has. */
    const val DAMPING = 0.55f
    const val STIFFNESS = 1500f
}
