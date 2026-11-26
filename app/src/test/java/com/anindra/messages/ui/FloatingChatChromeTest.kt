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
    fun discussionHeaderIsCenteredInsideARoundedRectangle() {
        val overlay = chat.substringAfter("internal fun FloatingChatHeader(")
            .substringBefore("internal fun MessageSelectionToolbar(")
        val toolbar = chat.substringAfter("internal fun ChatTopBar(")
            .substringBefore("internal fun ChatMessageList(")

        assertTrue(overlay.contains("Surface("))
        assertTrue(overlay.contains("shape = RoundedCornerShape(28.dp)"))
        assertTrue(overlay.contains("color = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(overlay.contains("shadowElevation = 4.dp"))
        assertTrue(toolbar.contains("CenterAlignedTopAppBar("))
        assertTrue(toolbar.contains("containerColor = Color.Transparent"))
    }

    @Test
    fun composerGroupsTheInputAndVoiceButtonInsideARoundedRectangle() {
        val composer = chat.substringAfter("internal fun InputBar(")
            .substringBefore("internal fun SuggestionChips(")

        assertTrue(composer.contains(".padding(horizontal = 12.dp, vertical = 8.dp)"))
        assertTrue(composer.contains("shape = RoundedCornerShape(28.dp)"))
        assertTrue(composer.contains("color = MaterialTheme.colorScheme.inputPill"))
        assertTrue(composer.contains("shape = RoundedCornerShape(32.dp)"))
        assertTrue(composer.contains("color = MaterialTheme.colorScheme.surfaceContainer,"))
        assertTrue(composer.contains("shadowElevation = 4.dp"))
        assertTrue(composer.contains("modifier = Modifier.fillMaxWidth().padding(4.dp)"))
    }

    @Test
    fun messagesUseTheFullViewportBehindTheFloatingChrome() {
        val screen = chat.substringAfter("fun ChatScreen(")
            .substringBefore("internal fun FloatingChatHeader(")
        val bottomBar = screen.substringAfter("bottomBar = {")
            .substringBefore(") { padding ->")

        assertTrue(screen.contains(") { padding ->\n        Box(Modifier.fillMaxSize())"))
        assertFalse(screen.contains("Box(Modifier.fillMaxSize().padding(padding))"))
        assertTrue(screen.contains("top = padding.calculateTopPadding() + 12.dp"))
        assertTrue(screen.contains("bottom = padding.calculateBottomPadding() + 12.dp"))
        assertTrue(bottomBar.contains("Modifier.navigationBarsPadding()"))
        assertFalse(
            bottomBar.substringBefore("AnimatedVisibility(").contains(".background(")
        )
    }

    @Test
    fun conversationSheetDoesNotShowARedundantBackButton() {
        assertTrue(activity.contains("showBackButton = !chatUsesSheet"))
        assertTrue(chat.contains("if (showBackButton) {\n                IconButton(onClick = onBack)"))
        assertFalse(activity.contains("showBackButton = false"))
    }

    @Test
    fun floatingBoundsRemainObservableForDeviceRegression() {
        assertTrue(chat.contains("\"header left=\$left top=\$top right=\$right bottom=\$bottom\""))
        assertTrue(chat.contains("\"composer left=\$left top=\$top right=\$right bottom=\$bottom\""))
        assertTrue(chat.contains("\"messages top=\$top bottom=\$bottom\""))
    }
}
