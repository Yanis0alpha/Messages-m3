package com.anindra.messages.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SimBadgeColorTest {
    private val source: String by lazy {
        val main = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "app/src/main") }
            .firstOrNull { it.isDirectory }
            ?: error("app/src/main not found")
        File(main, "java/com/anindra/messages/ui/ChatScreen.kt").readText()
    }

    /** Anchors on the declaration, not its visibility: the previews made this
     *  internal so the composable can be rendered in a preview pane, which is
     *  not something these colour assertions care about. */
    private val inputBar: String by lazy {
        val start = Regex("(?:private|internal|public)?\\s*fun InputBar\\(").find(source)?.range?.first
            ?: error("fun InputBar( not found")
        val end = Regex("(?:private|internal|public)?\\s*fun SimPickerDialog\\(").find(source)?.range?.first
            ?: error("fun SimPickerDialog( not found")
        assertTrue(start < end)
        source.substring(start, end)
    }

    private val simControl: String by lazy {
        inputBar.substringAfter("if (SimSwitcher.shouldShowSwitch(sims.size))")
            .substringBefore("if (showEmojiButton && attachEnabled)")
    }

    @Test
    fun simIconStaysTintedWithOnSurfaceVariant() {
        assertTrue(simControl.contains("tint = MaterialTheme.colorScheme.onSurfaceVariant"))
    }

    @Test
    fun simLabelUsesASurfaceColourForContrastOnTheIcon() {
        assertTrue(simControl.contains("color = MaterialTheme.colorScheme.surface"))
    }

    @Test
    fun simLabelNeverHardcodesWhiteOrPrimaryContainer() {
        assertFalse(simControl.contains("Color.White"))
        assertFalse(simControl.contains("onPrimaryContainer"))
    }
}
