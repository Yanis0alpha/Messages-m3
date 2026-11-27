package com.anindra.messages.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The voice control starts as an accent pill and becomes the right arch of a
 * connected recording group. The discard arch owns the red cancellation state.
 */
class ComposerRecordButtonTest {

    private val app: File by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "app") }
            .firstOrNull { File(it, "src").isDirectory }
            ?: error("app/src not found")
    }

    private val chat get() = File(
        app, "src/main/java/com/anindra/messages/ui/ChatScreen.kt"
    ).readText()

    @Test
    fun theIdleRecordButtonUsesTheAccent() {
        assertTrue(
            "the fill must be decided once, by the button's own state",
            chat.contains("val usesAccent = ComposerButton.usesAccentFill(circleMode, recording)")
        )
        assertTrue(
            "the idle record disc must be the accent, like Send",
            chat.contains("val fillTarget = if (usesAccent) accent")
        )
        assertTrue(
            "its glyph must be the on-accent ink",
            chat.contains("val inkTarget = if (usesAccent) MaterialTheme.colorScheme.onPrimary")
        )
        assertTrue(
            "the record disc must not fall back to the input pill's fill",
            !chat.contains("else MaterialTheme.colorScheme.inputPill")
        )
    }

    @Test
    fun voicePillMorphsIntoTwoOpposingArches() {
        assertTrue(chat.contains("VoiceArchGroup.IDLE_PILL_WIDTH_DP.dp"))
        assertTrue(chat.contains("topStart = voiceInnerCorner"))
        assertTrue(chat.contains("val discardShape = RoundedCornerShape("))
        assertTrue(chat.contains("visible = recording"))
        assertTrue(chat.contains("horizontalArrangement = Arrangement.spacedBy(deleteGap)"))
    }

    @Test
    fun swipingToDiscardTurnsOnlyTheDiscardArchRed() {
        assertTrue(
            "the voice control must keep its accent while recording",
            chat.contains("val usesAccent = ComposerButton.usesAccentFill(circleMode, recording)")
        )
        assertTrue(
            "the discard arch must own the error fill",
            chat.contains("if (discardArmed) MaterialTheme.colorScheme.error")
        )
        assertTrue(
            "the discard icon needs the matching readable ink",
            chat.contains("if (discardArmed) MaterialTheme.colorScheme.onError")
        )
    }

    @Test
    fun theInputPillShowsTheLiveVoiceWaveform() {
        assertTrue(
            "the composer must take the microphone level",
            chat.contains("voiceLevel: Float = 0f")
        )
        assertTrue(chat.contains("internal fun ComposerVoiceWaveform("))
        assertTrue(chat.contains("level = voiceLevel"))
        assertTrue(chat.contains("label = \"composerVoiceWaveformPulse\""))
    }

    @Test
    fun theCircleSwapsMeaningWithAMotion() {
        // The circle stays put and only the glyph turns over, which is what makes
        // the swap read as WhatsApp's: one button changing its mind.
        assertTrue(
            "the glyph must swap through AnimatedContent",
            chat.contains("AnimatedContent(") && chat.contains("label = \"composerGlyph\"")
        )
        assertTrue(
            "the swap must pinch and spring, not just cut",
            chat.contains("scaleIn(initialScale = ComposerSwap.SCALE")
        )
        assertTrue(
            "and the outgoing glyph must turn a quarter",
            chat.contains("ComposerSwap.TURN_DEGREES")
        )
    }

    @Test
    fun theSendCircleKeepsTheAccent() {
        assertTrue(
            "the send arrow must be drawn by the shared circle",
            chat.contains("ComposerGlyph.Send -> Icon(")
        )
        assertTrue(
            "its glyph is inked against the disc, which is the accent",
            chat.contains("tint = discInk")
        )
    }
}
