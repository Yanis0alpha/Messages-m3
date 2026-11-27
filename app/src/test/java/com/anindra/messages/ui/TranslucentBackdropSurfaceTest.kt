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
        assertTrue(surface.contains("if (blurEnabled)"))
        assertTrue(surface.contains("val surfaceAlpha = tintAlpha ?:"))
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
        assertTrue(settingsRow.contains("val cardTint = settingsCardTint()"))
        assertTrue(settingsRow.contains("tint = cardTint"))
        assertTrue(settingsRow.contains("blurEnabled = cardBlurEnabled"))
        assertTrue(settingsRow.contains("tintAlpha = cardAlpha"))
        val tintHelper = settings.substringAfter("internal fun settingsCardTint()")
            .substringBefore("/** \"#RRGGBB\"")
        assertTrue(tintHelper.contains("colors.primaryContainer"))
        assertFalse(tintHelper.contains("colors.secondaryContainer"))
        assertFalse(tintHelper.contains("colors.tertiaryContainer"))
        assertTrue(
            tintHelper.contains(
                "lerp(colors.primaryContainer, colors.surfaceContainerLow, 0.18f)"
            )
        )
        assertTrue(settings.contains("fun SettingsCategoryHeader(title: String)"))
        assertTrue(settings.contains("R.string.settings_category_appearance"))
        assertTrue(settings.contains("R.string.settings_category_organization"))
        assertTrue(settings.contains("R.string.settings_category_privacy_security"))
        assertTrue(settings.contains("R.string.settings_category_data"))
        assertTrue(settings.contains("icon: ImageVector? = null"))
        assertTrue(settings.contains("MaterialShapes.Cookie6Sided.toShape()"))
        assertTrue(settings.contains("icon = Icons.Rounded.Palette"))
        assertTrue(settings.contains("icon = Icons.Rounded.Tune"))
        assertTrue(settings.contains("internal fun SettingsExpressiveBackdrop("))
        assertTrue(settings.contains("SettingsExpressiveBackdrop()"))
        assertTrue(settings.contains("SettingsBlurredBackdropSource()"))
        assertTrue(settings.contains("DynamicBackdropLayer.SETTINGS_BLURRED_BACKGROUND"))
        assertTrue(settings.contains("DynamicBackdropLayer.SETTINGS_CARD"))
        assertTrue(settings.contains("blurRadius = 0.dp"))
        val groupedCards = settings.substringAfter("fun GroupedRowCard(")
            .substringBefore("/** Android's own notification prefs")
        assertTrue(groupedCards.contains("tint = settingsCardTint()"))
        assertTrue(groupedCards.contains("blurEnabled = LocalSettingsCardBlurEnabled.current"))
        assertTrue(groupedCards.contains("tintAlpha = LocalSettingsCardAlpha.current"))
        assertFalse(groupedCards.contains("tint = MaterialTheme.colorScheme.surfaceContainerLow"))
        assertTrue(settings.contains("MaterialShapes.Cookie12Sided.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Clover8Leaf.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Sunny.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Gem.toShape()"))
        assertTrue(settings.contains("MaterialShapes.Cookie9Sided.toShape()"))
    }

    @Test
    fun settingsSubPagesShareExpressiveGlassChromeAndCachedCardBlur() {
        val chrome = source("java/com/anindra/messages/ui/ExpressiveSettingsChrome.kt")
        val trash = source("java/com/anindra/messages/ui/TrashScreen.kt")
        val spam = source("java/com/anindra/messages/ui/SpamBlockedScreen.kt")
        val inbox = source("java/com/anindra/messages/ui/InboxSettingsScreen.kt")
        val advanced = source("java/com/anindra/messages/ui/AdvancedSettingsScreen.kt")
        val accessibility = source("java/com/anindra/messages/ui/AccessibilityScreen.kt")
        val about = source("java/com/anindra/messages/ui/AboutScreen.kt")
        val transfer = source("java/com/anindra/messages/ui/TransferLogScreen.kt")
        val scheduled = source("java/com/anindra/messages/ui/ScheduledMessagesScreen.kt")

        assertTrue(chrome.contains("internal fun ExpressiveSettingsScaffold("))
        assertTrue(chrome.contains("SettingsExpressiveBackdrop()"))
        assertTrue(chrome.contains("SettingsBlurredBackdropSource()"))
        assertTrue(chrome.contains("SettingsExpressiveBackdrop(registerAsSource = false)"))
        assertTrue(chrome.contains("dynamicBackdropEffect(\n                25.dp,\n                DynamicBackdropLayer.HEADER"))
        assertTrue(chrome.contains("tint = settingsCardTint()"))
        assertTrue(chrome.contains("blurRadius = 0.dp"))
        assertTrue(chrome.contains("backdropLayer = DynamicBackdropLayer.SETTINGS_CARD"))
        assertTrue(chrome.contains("blurEnabled = LocalSettingsCardBlurEnabled.current"))
        assertTrue(chrome.contains("tintAlpha = LocalSettingsCardAlpha.current"))

        listOf(trash, spam, inbox, advanced, accessibility, about, transfer, scheduled)
            .forEach { screen ->
                assertTrue(screen.contains("ExpressiveSettingsScaffold("))
            }
        assertTrue(trash.contains("ExpressiveSettingsCard("))
        assertTrue(spam.contains("ExpressiveSettingsCard("))
        assertTrue(scheduled.contains("ExpressiveSettingsCard("))
        assertTrue(trash.contains("Icons.Rounded.Delete"))
        assertTrue(trash.contains("Icons.Rounded.ChatBubble"))
        assertTrue(spam.contains("Icons.Rounded.Block"))
        assertTrue(spam.contains("Icons.Rounded.ChatBubble"))
    }

    @Test
    fun modernModalSheetsUseTransparentSameFrameBlur() {
        val chrome = source("java/com/anindra/messages/ui/ExpressiveSettingsChrome.kt")
        val inbox = source("java/com/anindra/messages/ui/InboxSettingsScreen.kt")
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val chatSheet = source("java/com/anindra/messages/ui/ConversationChatSheet.kt")
        val persistent = source("java/com/anindra/messages/ui/PersistentConversationSheet.kt")

        val sharedSheet = chrome.substringAfter("internal fun ExpressiveSettingsBottomSheet(")
        assertTrue(sharedSheet.contains("containerColor = Color.Transparent"))
        assertTrue(sharedSheet.contains("DynamicBackdropLayer.SHEET"))
        assertTrue(sharedSheet.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(inbox.contains("ExpressiveSettingsBottomSheet("))
        assertFalse(inbox.contains("ModalBottomSheet("))

        listOf(conversations, chatSheet, persistent).forEach { sheetOwner ->
            assertTrue(sheetOwner.contains("DynamicBackdropLayer.SHEET"))
            assertTrue(sheetOwner.contains("Color.Transparent"))
        }
    }
}
