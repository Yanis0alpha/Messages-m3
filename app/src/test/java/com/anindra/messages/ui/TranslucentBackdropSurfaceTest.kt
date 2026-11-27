package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertFalse
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
        assertFalse(surface.contains(".dynamicBackdropSource(backdropLayer)"))
        assertTrue(surface.contains(".dynamicBackdropEffect(blurRadius, backdropLayer, followMotion)"))
        assertTrue(surface.contains("tint.copy(alpha = surfaceAlpha)"))
    }

    @Test
    fun conversationContactsStartChatAndSettingsUseTheGlassSurface() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val newChat = source("java/com/anindra/messages/ui/NewChatScreen.kt")
        val settings = source("java/com/anindra/messages/ui/SettingsScreen.kt")
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

        val settingsRow = settings.substringAfter("fun SettingsRow(")
            .substringBefore("fun SettingsFooter(")
        assertTrue(settingsRow.contains("TranslucentBackdropSurface("))
        assertTrue(settingsRow.contains("tint = MaterialTheme.colorScheme.background"))
        assertTrue(settings.contains("internal fun SettingsExpressiveBackdrop("))
        assertTrue(settings.contains("SettingsExpressiveBackdrop()"))
        assertTrue(settings.contains("SettingsBlurredBackdropSource()"))
        assertTrue(settings.contains("DynamicBackdropLayer.SETTINGS_BLURRED_BACKGROUND"))
        assertTrue(settings.contains("DynamicBackdropLayer.SETTINGS_CARD"))
        assertTrue(settings.contains("blurRadius = 0.dp"))
        assertTrue(settings.contains("MaterialShapes.Cookie12Sided.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Clover8Leaf.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Sunny.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Gem.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Cookie9Sided.toShape()"))
    }
}
