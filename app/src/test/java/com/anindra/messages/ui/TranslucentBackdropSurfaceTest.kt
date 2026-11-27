package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslucentBackdropSurfaceTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun sharedSurfaceClipsAConfigurableTintOverBackdropBlur() {
        val surface = source("java/com/anindra/messages/ui/TranslucentBackdropSurface.kt")

        assertTrue(surface.contains(".clip(shape)"))
        assertTrue(surface.contains("BackdropCaptureGuard()"))
        assertTrue(surface.contains("DynamicBackdropBlur("))
        assertTrue(surface.contains("blurRadius = blurRadius"))
        assertTrue(surface.contains("tint.copy(alpha = surfaceAlpha)"))
    }

    @Test
    fun conversationContactsAndStartChatUseTheGlassSurface() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val newChat = source("java/com/anindra/messages/ui/NewChatScreen.kt")
        val conversationRow = conversations.substringAfter("internal fun ConversationRow(")
            .substringBefore("private enum class ConversationAction")
        val startChat = conversations.substringAfter("internal fun StartChatFab(")
            .substringBefore("private class UndoEntry")

        assertTrue(conversationRow.contains("TranslucentBackdropSurface("))
        assertTrue(conversationRow.contains("tint = pinnedTint"))
        assertTrue(conversationRow.contains("MaterialTheme.colorScheme.secondaryContainer"))
        assertTrue(conversationRow.contains("MaterialTheme.colorScheme.primaryContainer"))
        assertTrue(conversationRow.contains("MaterialTheme.colorScheme.tertiaryContainer"))
        assertTrue(startChat.contains("TranslucentBackdropSurface("))
        assertTrue(startChat.contains("tint = cs.startChatContainer"))
        assertTrue(newChat.contains("TranslucentBackdropSurface("))
        assertTrue(newChat.contains("tint = MaterialTheme.colorScheme.surfaceContainerLow"))
    }
}
