package com.anindra.messages.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Issue #481: an audio file can be attached, not only a voice note.
 *
 * The transport already handled `audio` — a voice note is stored that way — so
 * what was missing was the way in: an Audio item in the attachment carousel and
 * a picker that can reach music and recordings. The photo picker cannot, so the entry has
 * to go through the document picker.
 */
class AudioAttachmentTest {

    private val chat: String by lazy {
        val candidates = listOf(
            "app/src/main/java/com/anindra/messages/ui/ChatScreen.kt",
            "../app/src/main/java/com/anindra/messages/ui/ChatScreen.kt",
            "../../app/src/main/java/com/anindra/messages/ui/ChatScreen.kt"
        )
        candidates.firstNotNullOfOrNull { File(it).takeIf { f -> f.exists() }?.readText() }
            ?: error("ChatScreen.kt not found")
    }

    @Test
    fun theAttachmentCarouselOffersAudio() {
        val carousel = chat.substringAfter("AttachmentCarousel(")
        assertTrue(
            "the carousel must offer an Audio item",
            carousel.contains("R.string.chat_media_audio")
        )
        assertTrue(
            "and the item must be its own action with its own icon",
            carousel.contains("Icons.Rounded.Audiotrack")
        )
    }

    @Test
    fun audioIsPickedThroughTheDocumentPicker() {
        assertTrue(
            "the photo picker cannot return audio; the document picker can",
            chat.contains("ActivityResultContracts.OpenDocument()")
        )
        assertTrue(
            "the picker has to be filtered to audio",
            chat.contains("""audioLauncher.launch(arrayOf("audio/*"))""")
        )
    }

    @Test
    fun thePickedFileJoinsTheComposerTray() {
        val launcher = chat.substringAfter("val audioLauncher").substringBefore("fun leaveChat")
        assertTrue("the uri is added to pending media", launcher.contains("pendingMedia.add(uri)"))
    }
}
