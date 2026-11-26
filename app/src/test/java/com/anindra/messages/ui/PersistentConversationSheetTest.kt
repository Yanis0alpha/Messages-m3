package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertEquals
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

        val sheet = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")
        assertTrue(sheet.contains("BottomSheetScaffold("))
        assertTrue(sheet.contains("sheetPeekHeight = peekHeight"))
        assertTrue(sheet.contains(".height(sheetHeight)"))
        assertTrue(sheet.contains("sheetState.expand()"))
        assertTrue(sheet.contains("sheetState.partialExpand()"))
        assertTrue(sheet.contains("sheetDragHandle = null"))
        assertTrue(sheet.contains("BottomSheetDefaults.DragHandle()"))
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
    fun persistentSheetStaysBehindThePermanentNavigation() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val persistent = activity.indexOf("PersistentConversationSheet(")
        val navigation = activity.indexOf("MessagesNavigationBar(", persistent)
        assertTrue(persistent >= 0)
        assertTrue(navigation > persistent)
        assertTrue(activity.contains("navRoute in setOf(\"list\", \"chat\", \"settings\")"))
        assertTrue(activity.contains("sharedCaptureGuard = sharedCaptureGuard"))
        assertTrue(activity.contains("captureGuard = sharedCaptureGuard"))
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
