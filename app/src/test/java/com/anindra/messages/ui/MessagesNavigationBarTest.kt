package com.anindra.messages.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MessagesNavigationBarTest {

    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun destinationsStayInTheRequestedOrder() {
        assertEquals(
            listOf(
                TopLevelDestination.MESSAGES,
                TopLevelDestination.ARCHIVED,
                TopLevelDestination.SETTINGS
            ),
            TopLevelDestination.entries
        )
    }

    @Test
    fun barUsesMaterialThreeFloatingContainerAndStatefulIcons() {
        val bar = source("java/com/anindra/messages/ui/MessagesNavigationBar.kt")
        assertTrue(bar.contains("NavigationBar("))
        assertTrue(bar.contains("NavigationBarItem("))
        assertTrue(bar.contains("val barShape = RoundedCornerShape(32.dp)"))
        assertTrue(bar.contains("shape = barShape"))
        assertTrue(bar.contains("shadowElevation = 8.dp"))
        assertTrue(bar.contains("DynamicBackdropBlur("))
        assertTrue(bar.contains("val surfaceAlpha = LocalTranslucentSurfaceAlpha.current"))
        assertTrue(bar.contains("alpha = surfaceAlpha"))
        assertTrue(bar.contains("blurRadius = 32.dp"))
        assertTrue(bar.contains("captureGuard = captureGuard"))
        assertTrue(bar.contains("if (!captureGuard.active) drawContent()"))
        assertTrue(bar.contains("imageVector = if (selected) item.selectedIcon else item.unselectedIcon"))
        assertTrue(bar.contains("Icons.AutoMirrored.Filled.Chat"))
        assertTrue(bar.contains("Icons.Rounded.Archive"))
        assertTrue(bar.contains("Icons.Rounded.Settings"))
        assertTrue(bar.contains("R.string.navigation_settings"))
    }

    @Test
    fun topLevelScreensShareOneNavigationState() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val settings = source("java/com/anindra/messages/ui/SettingsScreen.kt")

        assertTrue(activity.contains("private var topLevelDestination by"))
        assertTrue(activity.contains("MessagesNavigationBar("))
        assertTrue(activity.contains("selectedDestination = if (navRoute == \"settings\")"))
        assertTrue(activity.contains("TopLevelDestination.SETTINGS"))
        assertTrue(activity.contains("onDestinationSelected = navigateTopLevel"))
        assertTrue(conversations.contains("val showArchived = selectedDestination == TopLevelDestination.ARCHIVED"))
        assertTrue(conversations.contains(".height(TopLevelNavigationBarClearance)"))
        assertFalse(settings.contains("bottomBar = {"))
        assertFalse(settings.contains(".navigationBarsPadding()"))
        assertTrue(settings.contains("SettingsLayout.BOTTOM_GAP + TopLevelNavigationBarClearance"))
    }

    @Test
    fun settingsRowsScrollBehindTheGlassNavigation() {
        val settings = source("java/com/anindra/messages/ui/SettingsScreen.kt")
        val scrollingColumn = settings.substringAfter(".verticalScroll(scrollState)")
        assertTrue(scrollingColumn.contains("SettingsRow("))
        assertTrue(
            scrollingColumn.contains(
                "SettingsLayout.BOTTOM_GAP + TopLevelNavigationBarClearance"
            )
        )
        assertFalse(settings.contains("bottomBar = {"))
    }
}
