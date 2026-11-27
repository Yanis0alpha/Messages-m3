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
        assertTrue(
            conversations.contains(
                "val showArchived = displayedDestination == TopLevelDestination.ARCHIVED"
            )
        )
        assertTrue(conversations.contains(".height(bottomControlsClearance)"))
        assertTrue(activity.contains(".padding(bottom = topLevelNavigationBottomOffset)"))
        assertFalse(settings.contains("bottomBar = {"))
        assertFalse(settings.contains(".navigationBarsPadding()"))
        assertTrue(settings.contains("SettingsLayout.BOTTOM_GAP + bottomOverlayClearance"))
    }

    @Test
    fun allTopLevelDestinationsUseTheSameZoomTransitionWithoutABlankFrame() {
        assertEquals(180, TopLevelDestinationTransition.OUTGOING_DURATION_MS)
        assertEquals(280, TopLevelDestinationTransition.INCOMING_DURATION_MS)
        assertEquals(0.88f, TopLevelDestinationTransition.MIN_SCALE, 0f)
        assertTrue(isTopLevelRouteTransition("list", "settings"))
        assertTrue(isTopLevelRouteTransition("chat", "settings"))
        assertTrue(isTopLevelRouteTransition("settings", "list"))
        assertTrue(isTopLevelRouteTransition("settings", "chat"))
        assertFalse(isTopLevelRouteTransition("list", "chat"))
        assertFalse(isTopLevelRouteTransition("settings", "advanced"))

        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val transition = activity.substringAfter("transitionSpec = {")
            .substringBefore("modifier = paneModifier")
        assertTrue(conversations.contains("var displayedDestination by remember"))
        assertTrue(conversations.contains("val topLevelContentScale = remember { Animatable(1f) }"))
        assertTrue(conversations.contains("displayedDestination = selectedDestination"))
        assertTrue(conversations.contains("scaleX = topLevelContentScale.value"))
        assertTrue(conversations.contains("scaleY = topLevelContentScale.value"))
        assertFalse(conversations.contains("alpha = topLevelContentAlpha.value"))
        val scaledScaffold = conversations.substringAfter("Scaffold(")
            .substringBefore("\n        TopLevelHeaderDock(")
        assertFalse(scaledScaffold.contains("TopLevelHeaderDock("))
        assertTrue(conversations.contains(".align(Alignment.TopCenter)\n                .zIndex(1f)"))
        assertTrue(
            conversations.contains(
                "Spacer(Modifier.height(TopLevelHeaderDockLayout.HEIGHT))"
            )
        )
        assertTrue(conversations.contains("val shapeEntrance = remember { Animatable("))
        assertTrue(conversations.contains("targetValue = 8f"))
        assertTrue(conversations.contains("rotationZ = shapeRotation.value"))
        assertTrue(conversations.contains("scaleX = 0.72f + (0.28f * shapeEntrance.value)"))
        assertTrue(
            conversations.contains(
                "TopLevelDestinationTransition.OUTGOING_DURATION_MS"
            )
        )
        assertTrue(
            conversations.contains(
                "TopLevelDestinationTransition.INCOMING_DURATION_MS"
            )
        )
        assertTrue(transition.contains("isTopLevelRouteTransition(initialState, targetState)"))
        assertTrue(transition.contains("scaleIn("))
        assertTrue(transition.contains("scaleOut("))
        assertTrue(transition.contains("TopLevelDestinationTransition.MIN_SCALE"))
        assertTrue(
            transition.indexOf("isTopLevelRouteTransition(initialState, targetState)") <
                transition.indexOf("slideInHorizontally(navSlide)")
        )
    }

    @Test
    fun settingsRowsScrollBehindTheGlassNavigation() {
        val settings = source("java/com/anindra/messages/ui/SettingsScreen.kt")
        val scrollingColumn = settings.substringAfter(".verticalScroll(scrollState)")
        assertTrue(scrollingColumn.contains("SettingsRow("))
        assertTrue(
            scrollingColumn.contains(
                "SettingsLayout.BOTTOM_GAP + bottomOverlayClearance"
            )
        )
        assertFalse(settings.contains("bottomBar = {"))
        assertFalse(settings.contains("topBar = {"))
        assertTrue(settings.contains("TopLevelHeaderDockLayout.HEIGHT + SettingsLayout.TOP_GAP"))
        assertTrue(settings.contains(".align(Alignment.TopCenter)"))
        assertTrue(settings.contains(".zIndex(1f)"))
    }
}
