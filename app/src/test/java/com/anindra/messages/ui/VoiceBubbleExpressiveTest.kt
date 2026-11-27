package com.anindra.messages.ui

import java.io.File
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
        assertTrue(source.contains("Icons.Rounded.PlayArrow"))
        assertTrue(source.contains("Icons.Rounded.Pause"))
    }

    @Test
    fun waveformIsTheOfficialExpressiveWavyProgressIndicator() {
        assertTrue(source.contains("LinearWavyProgressIndicator("))
        assertTrue(source.contains("progress = { animatedProgress }"))
        assertTrue(source.contains("Stroke(width = strokeWidth, cap = StrokeCap.Round)"))
        assertTrue(source.contains("val strokeWidth = with(LocalDensity.current) { 7.dp.toPx() }"))
        assertTrue(source.contains("gapSize = 5.dp"))
        assertTrue(source.contains("stopSize = 5.dp"))
        assertTrue(source.contains("amplitude = { if (reduceMotion) 0f else 1f }"))
        assertTrue(source.contains("wavelength = 30.dp"))
        assertFalse(source.contains("drawRoundRect("))
        assertFalse(source.contains("VoiceWaveform"))
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
