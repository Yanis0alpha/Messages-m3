package com.anindra.messages.ui

import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelHeaderDockTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun dockIsCompactButFitsTheSearchAndCenteredTitle() {
        assertEquals(168.dp, TopLevelHeaderDockLayout.HEIGHT)
        assertTrue(TopLevelHeaderDockLayout.HEIGHT > TopLevelHeaderDockLayout.TOP_CONTROL_HEIGHT)
        assertEquals(56.dp, TopLevelHeaderDockLayout.SEARCH_HEIGHT)
    }

    @Test
    fun titlesAndPlaceholdersEraseThenTypeTheirReplacement() {
        assertEquals(listOf("", "C", "Ch", "Cha", "Chat"), typewriterFrames("Chat"))
        assertEquals(14L, TopLevelHeaderDockLayout.ERASE_CHARACTER_DELAY_MS)
        assertEquals(22L, TopLevelHeaderDockLayout.TYPE_CHARACTER_DELAY_MS)

        val dock = source("java/com/anindra/messages/ui/TopLevelHeaderDock.kt")
        assertTrue(dock.contains("internal fun TypewriterSwapText("))
        assertTrue(dock.contains("typewriterFrames(displayedText)"))
        assertTrue(dock.contains(".dropLast(1)"))
        assertTrue(dock.contains("typewriterFrames(text).drop(1)"))
        assertTrue(dock.contains("placeholder = { TypewriterSwapText(text = placeholder) }"))
    }

    @Test
    fun dockIsEdgeAttachedRoundedOnlyBelowAndThirtyPercentGlass() {
        val dock = source("java/com/anindra/messages/ui/TopLevelHeaderDock.kt")

        assertEquals(0.30f, TopLevelHeaderDockLayout.TINT_ALPHA, 0f)
        assertTrue(dock.contains("bottomStart = TopLevelHeaderDockLayout.BOTTOM_CORNER"))
        assertTrue(dock.contains("bottomEnd = TopLevelHeaderDockLayout.BOTTOM_CORNER"))
        assertFalse(dock.contains("topStart = TopLevelHeaderDockLayout.BOTTOM_CORNER"))
        assertTrue(dock.contains(".fillMaxWidth()"))
        assertTrue(dock.contains("DynamicBackdropBlur("))
        assertTrue(dock.contains("blurRadius = 32.dp"))
        assertTrue(dock.contains("captureGuard = captureGuard"))
        assertTrue(dock.contains("if (!captureGuard.active) drawContent()"))
        assertTrue(dock.contains("alpha = TopLevelHeaderDockLayout.TINT_ALPHA"))
        assertTrue(dock.contains("contentAlignment = Alignment.Center"))
        assertTrue(dock.contains(".statusBarsPadding()"))
        assertTrue(dock.contains("shape = MaterialShapes.Cookie6Sided.toShape()"))
        assertTrue(dock.contains("modifier = Modifier.size(20.dp)"))
    }

    @Test
    fun messagesArchivesAndSettingsUseTheSameSearchDock() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val settings = source("java/com/anindra/messages/ui/SettingsScreen.kt")

        assertTrue(conversations.contains("TopLevelHeaderDock("))
        assertTrue(conversations.contains("TopLevelDockSearchField("))
        assertTrue(conversations.contains("R.string.conversations_messages"))
        assertTrue(conversations.contains("R.string.conversations_archived"))
        assertTrue(conversations.contains("R.string.conversations_archived_search"))
        assertTrue(conversations.contains("contentWindowInsets = WindowInsets(0, 0, 0, 0)"))
        assertTrue(settings.contains("TopLevelHeaderDock("))
        assertTrue(settings.contains("TopLevelDockSearchField("))
        assertTrue(settings.contains("R.string.navigation_settings"))
        assertTrue(settings.contains("contentWindowInsets = WindowInsets(0, 0, 0, 0)"))
        assertTrue(settings.contains("LocalSettingsRowSearchQuery provides settingsQuery"))
        assertTrue(settings.contains("SettingsSearch.matches(rowSearchQuery, title)"))
    }
}
