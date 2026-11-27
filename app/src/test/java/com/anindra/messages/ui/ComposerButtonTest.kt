package com.anindra.messages.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The composer's voice pill, recording arch and send control keep the accent.
 * Cancellation owns the red state on its separate discard arch.
 */
class ComposerButtonTest {

    @Test
    fun anEmptyComposerIsTheRecordCircle() {
        assertEquals(ComposerCircle.Record, ComposerButton.modeFor(micMode = true))
    }

    @Test
    fun typingSwapsTheCircleToSend() {
        assertEquals(ComposerCircle.Send, ComposerButton.modeFor(micMode = false))
    }

    @Test
    fun theRecordCircleDrawsTheWave() {
        assertEquals(ComposerGlyph.Wave, ComposerButton.glyphFor(ComposerCircle.Record))
    }

    @Test
    fun theSendCircleDrawsTheArrow() {
        assertEquals(ComposerGlyph.Send, ComposerButton.glyphFor(ComposerCircle.Send))
    }

    @Test
    fun sendAndAnIdleRecordShareTheAccentFill() {
        assertTrue(ComposerButton.usesAccentFill(ComposerCircle.Send, recording = false))
        assertTrue(ComposerButton.usesAccentFill(ComposerCircle.Send, recording = true))
        assertTrue(ComposerButton.usesAccentFill(ComposerCircle.Record, recording = false))
    }

    @Test
    fun aRunningNoteKeepsTheVoiceArchOnTheAccent() {
        assertTrue(ComposerButton.usesAccentFill(ComposerCircle.Record, recording = true))
    }

    @Test
    fun theSwapIsAQuarterTurnWithAPinch() {
        assertEquals(90f, ComposerSwap.TURN_DEGREES, 0f)
        assertEquals(0.5f, ComposerSwap.SCALE, 0f)
        assertTrue(ComposerSwap.DAMPING > 0f && ComposerSwap.DAMPING < 1f)
    }
}
