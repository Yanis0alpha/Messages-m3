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
    }

    @Test
    fun messagesArchivesAndSettingsUseTheSameSearchDock() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val settings = source("java/com/anindra/messages/ui/SettingsScreen.kt")

        assertTrue(conversations.contains("TopLevelHeaderDock("))
        assertTrue(conversations.contains("TopLevelDockSearchField("))
        assertTrue(conversations.contains("R.string.conversations_messages"))
        assertTrue(conversations.contains("R.string.conversations_archived"))
        assertTrue(settings.contains("TopLevelHeaderDock("))
        assertTrue(settings.contains("TopLevelDockSearchField("))
        assertTrue(settings.contains("R.string.navigation_settings"))
        assertTrue(settings.contains("LocalSettingsRowSearchQuery provides settingsQuery"))
        assertTrue(settings.contains("SettingsSearch.matches(rowSearchQuery, title)"))
    }
}
