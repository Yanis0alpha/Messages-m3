package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingChatChromeTest {
    private val root: File by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "app/src/main").isDirectory }
    }

    private val chat: String by lazy {
        File(root, "app/src/main/java/com/anindra/messages/ui/ChatScreen.kt").readText()
    }

    private val activity: String by lazy {
        File(root, "app/src/main/java/com/anindra/messages/MainActivity.kt").readText()
    }

    @Test
    fun discussionHeaderIsAnInsetRoundedSurface() {
        val header = chat.substringAfter("internal fun FloatingChatHeader(")
            .substringBefore("internal fun MessageSelectionToolbar(")

        assertTrue(header.contains(".padding(horizontal = 12.dp, vertical = 8.dp)"))
        assertTrue(header.contains("shape = RoundedCornerShape(28.dp)"))
        assertTrue(header.contains("color = MaterialTheme.colorScheme.chatBar"))
        assertTrue(header.contains("shadowElevation = 4.dp"))
        assertTrue(chat.contains("FloatingChatHeader {\n            AnimatedContent("))
    }

    @Test
    fun composerIsAnInsetRoundedSurface() {
        val composer = chat.substringAfter("internal fun InputBar(")
            .substringBefore("internal fun SuggestionChips(")

        assertTrue(composer.contains(".padding(horizontal = 12.dp, vertical = 8.dp)"))
        assertTrue(composer.contains("shape = RoundedCornerShape(32.dp)"))
        assertTrue(composer.contains("color = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(composer.contains("shadowElevation = 4.dp"))
        assertTrue(composer.contains("modifier = Modifier.fillMaxWidth().padding(4.dp)"))
    }

    @Test
    fun conversationSheetDoesNotShowARedundantBackButton() {
        assertTrue(activity.contains("showBackButton = !chatUsesSheet"))
        assertTrue(chat.contains("if (showBackButton) {\n                IconButton(onClick = onBack)"))
        assertFalse(activity.contains("showBackButton = false"))
    }

    @Test
    fun floatingBoundsRemainObservableForDeviceRegression() {
        assertTrue(chat.contains("\"header left=\$left right=\$right\""))
        assertTrue(chat.contains("\"composer left=\$left right=\$right\""))
    }
}
