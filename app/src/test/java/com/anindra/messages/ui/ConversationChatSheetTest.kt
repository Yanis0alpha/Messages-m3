package com.anindra.messages.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConversationChatSheetTest {

    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun sheetStopsAtNinetyPercent() {
        assertEquals(0.9f, ChatSheetLayout.HEIGHT_FRACTION, 0f)
    }

    @Test
    fun sheetIsMaterialThreeAndCanBeDraggedClosed() {
        val sheet = source("java/com/anindra/messages/ui/ConversationChatSheet.kt")
        assertTrue(sheet.contains("ModalBottomSheet("))
        assertTrue(sheet.contains("skipPartiallyExpanded = true"))
        assertTrue(sheet.contains("fillMaxHeight(ChatSheetLayout.HEIGHT_FRACTION)"))
        assertTrue(sheet.contains("sheetState.hide()"))
        assertTrue(sheet.contains("BottomSheetDefaults.DragHandle()"))
        assertTrue(sheet.contains("RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)"))
    }

    @Test
    fun compactChatKeepsTheListBehindTheSheet() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        assertTrue(activity.contains("val chatUsesSheet = !twoPaneLayout && !bubbleMode"))
        assertTrue(activity.contains("target == \"chat\" && chatUsesSheet"))
        assertTrue(activity.contains("onDismissed = leaveChat"))
        assertTrue(activity.contains("onGestureDismiss = leaveChatByGesture"))
        assertTrue(activity.contains("chatContent(requestDismiss)"))
        assertTrue(
            activity.contains(
                "chatUsesSheet &&\n                                        " +
                    "(initialState == \"chat\" || targetState == \"chat\")"
            )
        )
    }

    @Test
    fun expandedAndBubbleChatsKeepTheirDedicatedPane() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        assertTrue(activity.contains("\"chat\" -> chatContent(leaveChat)"))
    }

    @Test
    fun aSwipeDismissStillPersistsTheLatestDraft() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        assertTrue(chat.contains("SideEffect { onDraftSnapshot(draft) }"))
        assertTrue(activity.contains("vm.saveDraftAndMaybeTrash(chatId, chatDraftSnapshot.trim())"))
        assertTrue(activity.contains("onGestureDismiss = leaveChatByGesture"))
    }
}
