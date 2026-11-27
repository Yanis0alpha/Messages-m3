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
        assertEquals(0.5f, PersistentConversationSheetLayout.DISMISS_PREVIEW_THRESHOLD, 0f)
        assertEquals(-2.5f, PersistentConversationSheetLayout.DISMISS_CONTENT_ROTATION, 0f)
        assertEquals(0.72f, PersistentConversationSheetLayout.EMPTY_STATE_INITIAL_SCALE, 0f)
        assertEquals(8f, PersistentConversationSheetLayout.EMPTY_STATE_INITIAL_ROTATION, 0f)
        assertEquals(20.dp, PersistentConversationSheetLayout.DISMISS_CONTENT_TRAVEL)
        assertEquals(24.dp, PersistentConversationSheetLayout.EMPTY_STATE_TRAVEL)
        assertEquals(0f, persistentSheetCollapseProgress(100f, 100f, 900f), 0f)
        assertEquals(0.5f, persistentSheetCollapseProgress(500f, 100f, 900f), 0f)
        assertEquals(1f, persistentSheetCollapseProgress(900f, 100f, 900f), 0f)
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
        assertTrue(sheet.contains("coordinates.positionInRoot().y"))
        assertTrue(sheet.contains("persistentSheetCollapseProgress("))
        assertTrue(sheet.contains("collapseProgress >="))
        assertTrue(sheet.contains("dragDismissPreviewVisible"))
        assertTrue(sheet.contains("label = \"persistentConversationEmptyState\""))
        assertTrue(sheet.contains("animationSpec = motionSpring(LocalReduceMotion.current)"))
        assertFalse(sheet.contains("DISMISS_CONTENT_SCALE"))
        assertTrue(sheet.contains("val minimizeScale = if (showMinimizedSummary)"))
        assertTrue(sheet.contains("DISMISS_CONTENT_ROTATION * dismissProgress"))
        assertTrue(sheet.contains("translationY = dismissTravelPx * dismissProgress"))
        assertTrue(sheet.contains("EMPTY_STATE_INITIAL_ROTATION * (1f - progress)"))
        assertTrue(sheet.contains("translationY = emptyStateTravelPx * (1f - progress)"))
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
        val activity = source("java/com/anindra/messages/MainActivity.kt")
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
        assertTrue(sheet.contains("showMinimizedSummary = true"))
        assertTrue(
            sheet.contains(
                "targetValue = if (showMinimizedSummary || dragDismissPreviewVisible) 0f else 1f"
            )
        )
        assertTrue(sheet.contains("PersistentConversationHandleMode.MINIMIZED"))
        assertTrue(sheet.contains("MinimizedConversationPill("))
        assertTrue(sheet.contains("conversationTitle: String"))
        assertTrue(sheet.contains("conversationAddress: String"))
        assertTrue(sheet.contains(".clickable(onClick = restoreMinimizedConversation)"))
        assertTrue(sheet.contains("showMinimizedSummary = false"))
        assertTrue(sheet.contains("// Keep ChatScreen composed"))
        assertTrue(activity.contains("vm.conversationById(chatId)"))
        assertTrue(activity.contains("conversationTitle = sheetConversationTitle"))
        assertTrue(activity.contains("conversationAddress = sheetConversationAddress"))
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
    fun tenPercentSheetStaysVisibleOnMessagesArchivesAndSettings() {
        assertTrue(
            shouldShowPersistentConversationSheet(
                useNewUi = true,
                chatUsesSheet = true,
                navRoute = "list"
            )
        )
        assertTrue(
            shouldShowPersistentConversationSheet(
                useNewUi = true,
                chatUsesSheet = true,
                navRoute = "list"
            )
        )
        assertTrue(
            shouldShowPersistentConversationSheet(
                useNewUi = true,
                chatUsesSheet = true,
                navRoute = "settings"
            )
        )

        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val navigation = activity.substringAfter(
            "val navigateTopLevel: (TopLevelDestination) -> Unit"
        ).substringBefore("val chatUsesSheet")
        val sheet = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        assertTrue(navigation.contains("vm.saveDraftAndMaybeTrash"))
        assertTrue(navigation.contains("TopLevelDestination.ARCHIVED"))
        assertTrue(navigation.contains("navRoute = \"list\""))
        assertTrue(navigation.contains("TopLevelDestination.SETTINGS -> navRoute = \"settings\""))
        assertTrue(navigation.contains("if (retainedMinimizedConversation)"))
        assertTrue(activity.contains("val persistentSheetComposed = persistentSheetVisible ||"))
        assertTrue(activity.contains("navRoute == \"chat\" || retainedMinimizedConversation"))
        assertTrue(activity.contains("visible = persistentSheetVisible"))
        assertTrue(activity.contains("conversationKey = chatId"))
        assertTrue(activity.contains("retainedMinimizedConversation = it"))
        val restore = activity.substringAfter("onRestoreConversation = {")
            .substringBefore("}\n                            )")
        assertTrue(restore.contains("topLevelDestination = TopLevelDestination.MESSAGES"))
        assertTrue(restore.contains("navRoute = \"chat\""))
        assertTrue(sheet.contains("latestOnRestoreConversation()"))
        assertTrue(sheet.contains("if (visible) placeable.placeRelative(0, 0)"))
        assertTrue(sheet.contains("latestOnMinimizedStateChanged(true)"))
        assertTrue(activity.contains("chatContent(leaveChat, persistentSheetVisible)"))
        assertTrue(chat.contains("interactionEnabled: Boolean = true"))
        assertTrue(chat.contains("interactionEnabled && backHandled"))
        assertTrue(chat.contains("if (!interactionEnabled) focusManager.clearFocus(force = true)"))
        assertTrue(activity.contains("shouldShowPersistentConversationSheet("))
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
    fun navigationAndSheetUseOrderedDynamicBackdropLayersWithoutSheetFeedback() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val navigation = source("java/com/anindra/messages/ui/MessagesNavigationBar.kt")
        val sheet = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")
        assertTrue(activity.contains("DynamicBackdropHost {"))
        assertTrue(
            navigation.contains(".dynamicBackdropSource(DynamicBackdropLayer.NAVIGATION)")
        )
        assertTrue(sheet.contains("DynamicBackdropLayer.SHEET"))
        assertFalse(sheet.contains(".dynamicBackdropSource(DynamicBackdropLayer.SHEET)"))
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
