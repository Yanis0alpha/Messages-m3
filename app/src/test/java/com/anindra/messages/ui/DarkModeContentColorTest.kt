package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkModeContentColorTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun transparentHomeSurfacesUseDarkModeAwareContentColors() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val actionSheet = conversations.substringAfter("if (sheetConvo != null)")
            .substringBefore("permanentDeleteTarget?.let")

        assertTrue(
            activity.contains(
                "LocalContentColor provides MaterialTheme.colorScheme.onBackground"
            )
        )
        assertTrue(conversations.contains("contentColor = MaterialTheme.colorScheme.onBackground"))
        assertTrue(conversations.contains("color = MaterialTheme.colorScheme.onBackground"))
        assertTrue(conversations.contains("tint = MaterialTheme.colorScheme.onBackground"))
        assertTrue(actionSheet.contains("color = MaterialTheme.colorScheme.onSurface"))
    }

    @Test
    fun chatHeaderAndSearchUseOnSurfaceColors() {
        val chat = source("java/com/anindra/messages/ui/ChatScreen.kt")
        val search = chat.substringAfter("internal fun ChatSearchBar(")
            .substringBefore("internal fun ChatTopBar(")
        val topBar = chat.substringAfter("internal fun ChatTopBar(")
            .substringBefore("internal fun ChatMessageList(")

        assertTrue(chat.contains("contentColor = MaterialTheme.colorScheme.onBackground"))
        assertTrue(chat.contains("contentColor = MaterialTheme.colorScheme.onSurface"))
        assertTrue(search.contains("focusedTextColor = MaterialTheme.colorScheme.onSurface"))
        assertTrue(search.contains("navigationIconContentColor = MaterialTheme.colorScheme.onSurface"))
        assertTrue(topBar.contains("titleContentColor = MaterialTheme.colorScheme.onSurface"))
        assertTrue(topBar.contains("color = MaterialTheme.colorScheme.onSurface"))
    }
}
