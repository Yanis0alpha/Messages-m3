package com.anindra.messages.ui

import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceBubbleExpressiveTest {
    private val source: String by lazy {
        val main = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "app/src/main") }
            .first { it.isDirectory }
        File(main, "java/com/anindra/messages/ui/VoiceBubble.kt").readText()
    }

    @Test
    fun playControlMorphsFromArchToCookie() {
        assertTrue(source.contains("Morph(MaterialShapes.Arch, MaterialShapes.Cookie6Sided)"))
        assertTrue(source.contains("morph.toPath(progress.coerceIn(0f, 1f))"))
        assertTrue(source.contains("targetValue = if (playing) 1f else 0f"))
        assertTrue(source.contains("label = \"voiceBubbleControlMorph\""))
        assertTrue(source.contains("shape = controlShape"))
        assertEquals(-90f, VoiceBubbleLayout.ARCH_LEFT_ROTATION_DEGREES, 0f)
        assertTrue(source.contains("rotateZ(rotationDegrees)"))
        assertTrue(source.contains("Icons.Rounded.PlayArrow"))
        assertTrue(source.contains("Icons.Rounded.Pause"))
    }

    @Test
    fun waveformIsTheOfficialExpressiveWavyProgressIndicator() {
        assertTrue(source.contains("LinearWavyProgressIndicator("))
        assertTrue(source.contains("progress = { animatedProgress }"))
        assertTrue(source.contains("Stroke(width = strokeWidth, cap = StrokeCap.Round)"))
        assertTrue(source.contains("VoiceBubbleLayout.THIN_PROGRESS_STROKE.toPx()"))
        assertTrue(source.contains(".height(VoiceBubbleLayout.THIN_PROGRESS_HEIGHT)"))
        assertTrue(source.contains("gapSize = VoiceBubbleLayout.PROGRESS_GAP"))
        assertTrue(source.contains("stopSize = VoiceBubbleLayout.STOP_INDICATOR_SIZE"))
        assertTrue(source.contains("amplitude = { if (reduceMotion) 0f else 1f }"))
        assertTrue(source.contains("wavelength = VoiceBubbleLayout.BASELINE_WAVELENGTH"))
        assertFalse(source.contains("drawRoundRect("))
        assertFalse(source.contains("VoiceWaveform"))
    }

    @Test
    fun bubbleGrowsTowardTheLeftAndUsesBaselineThinMetrics() {
        assertEquals(280.dp, VoiceBubbleLayout.MIN_WIDTH)
        assertEquals(300.dp, VoiceBubbleLayout.MAX_WIDTH)
        assertEquals(4.dp, VoiceBubbleLayout.THIN_PROGRESS_STROKE)
        assertEquals(10.dp, VoiceBubbleLayout.THIN_PROGRESS_HEIGHT)
        assertEquals(4.dp, VoiceBubbleLayout.PROGRESS_GAP)
        assertEquals(4.dp, VoiceBubbleLayout.STOP_INDICATOR_SIZE)
        assertEquals(40.dp, VoiceBubbleLayout.BASELINE_WAVELENGTH)
        assertTrue(source.contains("min = VoiceBubbleLayout.MIN_WIDTH"))
        assertTrue(source.contains("max = VoiceBubbleLayout.MAX_WIDTH"))
    }

    @Test
    fun durationLivesInATrailingPill() {
        val duration = source.substringAfter("LinearWavyProgressIndicator(")
            .substringBefore("if (body.text.isNotBlank())")
        assertTrue(duration.contains("shape = CircleShape"))
        assertTrue(duration.contains("text = lengthText"))
        assertTrue(duration.contains("padding(horizontal = 8.dp, vertical = 4.dp)"))
    }
}
