package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentCarouselWiringTest {
    private val chat: String by lazy {
        val app = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "app") }
            .first { File(it, "src").isDirectory }
        File(app, "src/main/java/com/anindra/messages/ui/ChatScreen.kt").readText()
    }

    private val bottomBar: String
        get() = chat.substringAfter("bottomBar = {").substringBefore(") { padding ->")

    @Test
    fun attachButtonOpensCarouselAboveComposer() {
        assertTrue(bottomBar.contains("AttachmentCarousel("))
        assertTrue(bottomBar.contains("InputBar("))
        assertTrue(
            bottomBar.indexOf("AttachmentCarousel(") < bottomBar.indexOf("InputBar(")
        )
        assertTrue(bottomBar.contains("visible = attachOpen && !selectionActive"))
    }

    @Test
    fun carouselOffersEveryExistingAction() {
        val carousel = bottomBar.substringAfter("AttachmentCarousel(")
        for (resource in listOf(
            "chat_media_camera",
            "chat_media_gallery",
            "chat_media_audio",
            "chat_media_location",
            "chat_media_contact"
        )) {
            assertTrue("$resource must remain available", carousel.contains("R.string.$resource"))
        }
    }

    @Test
    fun carouselDoesNotDismissTheKeyboard() {
        val attach = bottomBar.substringAfter("onAttach = {").substringBefore("attachOpen = attachOpen")
        assertTrue(attach.contains("attachOpen = !attachOpen"))
        assertTrue(attach.contains("showEmoji = false"))
        assertFalse(attach.contains("clearFocus()"))
        assertFalse(attach.contains("keyboard?.hide()"))
        assertTrue(bottomBar.contains("composerBottomInset = keyboardInset"))
    }

    @Test
    fun plusUsesTheMaterial3CookieShape() {
        val input = chat.substringAfter("internal fun InputBar(")
            .substringBefore("internal fun SuggestionChips(")
        assertTrue(input.contains("shape = MaterialShapes.Cookie6Sided.toShape()"))
        assertTrue(input.contains("onClick = onAttach"))
        assertTrue(input.contains("Icons.Rounded.Add"))
        assertTrue(input.contains("targetValue = if (attachOpen) 45f else 0f"))
    }

    @Test
    fun backClosesCarouselBeforeLeavingChat() {
        assertTrue(chat.contains("BackHandler(enabled = attachOpen) { attachOpen = false }"))
        val leaveChat = chat.indexOf("BackHandler(enabled = backHandled, onBack = ::leaveChat)")
        val carouselBack = chat.indexOf("BackHandler(enabled = attachOpen)")
        assertTrue(carouselBack > leaveChat)
    }

    @Test
    fun obsoleteAttachmentSheetIsAbsent() {
        assertFalse(chat.contains("fun AttachSheet("))
    }
}
