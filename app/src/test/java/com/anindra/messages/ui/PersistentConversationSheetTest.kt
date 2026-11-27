package com.anindra.messages.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentConversationSheetTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun sheetKeepsATenPercentPeekAndExpandsToNinetyPercent() {
        assertEquals(0.1f, PersistentConversationSheetLayout.PEEK_FRACTION, 0f)
        assertEquals(0.9f, PersistentConversationSheetLayout.EXPANDED_FRACTION, 0f)
        assertEquals(3, PersistentConversationSheetLayout.ACTION_TIMEOUT_SECONDS)
        assertEquals(1_000L, PersistentConversationSheetLayout.ACTION_TICK_MS)
        assertEquals(80.dp, PersistentConversationSheetLayout.peekHeight(800.dp))

        val sheet = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")
        assertTrue(sheet.contains("BottomSheetScaffold("))
        assertTrue(sheet.contains("sheetPeekHeight = peekHeight"))
        assertTrue(sheet.contains(".height(sheetHeight)"))
        assertTrue(sheet.contains("sheetState.expand()"))
        assertTrue(sheet.contains("sheetState.partialExpand()"))
        assertTrue(sheet.contains("sheetDragHandle = null"))
        assertTrue(sheet.contains("BottomSheetDefaults.DragHandle()"))
        assertTrue(
            sheet.contains("MaterialTheme.colorScheme.surfaceContainerHigh.copy(")
        )
        assertTrue(
            sheet.contains("alpha = surfaceAlpha")
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun aSettledDownwardDragClosesTheOpenConversation() {
        assertTrue(
            shouldClosePersistentConversation(
                conversationOpen = true,
                collapseArmed = true,
                keepConversationOpen = false,
                currentValue = SheetValue.PartiallyExpanded,
                targetValue = SheetValue.PartiallyExpanded,
                animationRunning = false
            )
        )
        assertFalse(
            shouldClosePersistentConversation(
                conversationOpen = true,
                collapseArmed = true,
                keepConversationOpen = false,
                currentValue = SheetValue.PartiallyExpanded,
                targetValue = SheetValue.PartiallyExpanded,
                animationRunning = true
            )
        )

        val sheet = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        assertTrue(sheet.contains("snapshotFlow {"))
        assertTrue(sheet.contains("sheetState.isAnimationRunning"))
        assertTrue(sheet.contains("}.collect { snapshot ->"))
        assertTrue(sheet.contains("expandedConversation = false"))
        assertTrue(sheet.contains("rememberUpdatedState(onCollapsed)"))
        assertTrue(sheet.contains("latestOnCollapsed()"))
        assertTrue(activity.contains("onCollapsed = leaveChatByGesture"))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun handleMorphsIntoCloseAndMinimizeActions() {
        assertFalse(
            shouldClosePersistentConversation(
                conversationOpen = true,
                collapseArmed = true,
                keepConversationOpen = true,
                currentValue = SheetValue.PartiallyExpanded,
                targetValue = SheetValue.PartiallyExpanded,
                animationRunning = false
            )
        )

        val sheet = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")
        val strings = source("res/values/strings_conversations.xml")
        val french = source("res/values-fr/strings_conversations.xml")
        val close = sheet.substringAfter("onClose = {")
            .substringBefore("onMinimize = {")
        val minimize = sheet.substringAfter("onMinimize = {")
            .substringBefore("}\n                        )")

        assertTrue(sheet.contains("PersistentConversationSheetHandle("))
        assertTrue(sheet.contains("AnimatedContent("))
        assertTrue(sheet.contains("ConnectedButtonGroupLayout.GAP"))
        assertTrue(sheet.contains("LaunchedEffect(handleActionsVisible, conversationOpen)"))
        assertTrue(sheet.contains("delay(PersistentConversationSheetLayout.ACTION_TICK_MS)"))
        assertTrue(sheet.contains("actionCountdownSeconds = remaining"))
        assertTrue(sheet.contains("R.string.conversation_sheet_actions_timeout"))
        assertTrue(sheet.contains("R.string.common_close"))
        assertTrue(sheet.contains("R.string.conversation_sheet_minimize"))
        assertTrue(sheet.contains("PersistentConversationSheetMode.MINIMIZE_REQUESTED"))
        assertTrue(sheet.contains("PersistentConversationSheetMode.MINIMIZED"))
        assertTrue(sheet.contains("scope.launch { sheetState.partialExpand() }"))
        assertTrue(sheet.contains("sheetMode != PersistentConversationSheetMode.ACTIVE"))
        assertTrue(sheet.contains("restoreMinimizedConversation()"))
        assertTrue(sheet.contains("sheetMode == PersistentConversationSheetMode.MINIMIZED"))
        assertTrue(sheet.contains(".clickable(onClick = restoreMinimizedConversation)"))
        assertTrue(close.contains("expandedConversation = false"))
        assertTrue(close.contains("latestOnCollapsed()"))
        assertFalse(minimize.contains("latestOnCollapsed()"))
        assertTrue(strings.contains("name=\"conversation_sheet_minimize\">Minimize"))
        assertTrue(strings.contains("name=\"conversation_sheet_actions_timeout\""))
        assertTrue(french.contains("name=\"conversation_sheet_minimize\">Minimiser"))
        assertTrue(french.contains("name=\"conversation_sheet_actions_timeout\""))
    }

    @Test
    fun emptySheetUsesTheRequestedCookieNineState() {
        val sheet = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")
        assertTrue(sheet.contains("MaterialShapes.Cookie9Sided.toShape()"))
        assertTrue(sheet.contains("Icons.AutoMirrored.Rounded.Chat"))
        assertTrue(sheet.contains("R.string.conversation_sheet_nothing_open"))
        assertTrue(sheet.contains("if (conversationOpen)"))
        assertTrue(sheet.contains("PersistentConversationEmptyState()"))
    }

    @Test
    fun persistentPeekNavigationAndFabFormAVerticalStack() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val navigation = activity.indexOf("MessagesNavigationBar(")
        val persistent = activity.indexOf("PersistentConversationSheet(", navigation)
        assertTrue(navigation >= 0)
        assertTrue(persistent > navigation)
        assertTrue(activity.contains("navRoute in setOf(\"list\", \"chat\", \"settings\")"))
        assertTrue(activity.contains("TopLevelNavigationBarClearance + topLevelNavigationBottomOffset"))
        assertTrue(activity.contains(".padding(bottom = topLevelNavigationBottomOffset)"))
        assertTrue(activity.contains("bottomControlsClearance = topLevelControlsClearance"))
        assertTrue(conversations.contains(".height(bottomControlsClearance)"))
        assertTrue(conversations.contains(".padding(bottom = bottomControlsClearance)"))
    }

    @Test
    fun navigationAndSheetCapturesCannotHideEachOther() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        assertTrue(activity.contains("val navigationCaptureGuard = remember"))
        assertTrue(activity.contains("val persistentSheetCaptureGuard = remember"))
        assertTrue(activity.contains("sharedCaptureGuard = navigationCaptureGuard"))
        assertTrue(activity.contains("captureGuard = persistentSheetCaptureGuard"))
    }

    @Test
    fun raisedNavigationDoesNotAddTheSystemInsetToThePeekGap() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val navigation = source("java/com/anindra/messages/ui/MessagesNavigationBar.kt")
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")

        assertTrue(activity.contains("applyNavigationBarsPadding = !persistentSheetVisible"))
        assertTrue(navigation.contains("if (applyNavigationBarsPadding)"))
        assertTrue(navigation.contains(".then(systemBarPadding)"))
        assertTrue(conversations.contains("if (applyNavigationBarsPadding)"))
    }

    @Test
    fun backgroundUsesSeveralMaterialThreeShapes() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        assertTrue(conversations.contains("HomeShapeBackdrop()"))
        assertTrue(conversations.contains("MaterialShapes.Cookie12Sided.toShape()"))
        assertTrue(conversations.contains("MaterialShapes.Clover8Leaf.toShape()"))
        assertTrue(conversations.contains("MaterialShapes.Sunny.toShape()"))
        assertTrue(conversations.contains("MaterialShapes.Gem.toShape()"))
        assertTrue(conversations.contains("containerColor = Color.Transparent"))
    }
}
