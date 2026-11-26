package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentCarouselTest {
    private val epsilon = 0.01f

    private val source: String by lazy {
        val app = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "app") }
            .first { File(it, "src/main").isDirectory }
        File(app, "src/main/java/com/anindra/messages/ui/AttachmentCarousel.kt").readText()
    }

    @Test
    fun carouselUsesTheMaterial3UncontainedLayout() {
        assertTrue(source.contains("HorizontalUncontainedCarousel("))
        assertTrue(source.contains("rememberCarouselState { items.size }"))
        assertTrue(source.contains(".maskClip(MaterialTheme.shapes.extraLarge)"))
    }

    @Test
    fun cameraLeadsTheExistingAttachmentActions() {
        assertTrue(
            source.contains(
                "val items = listOf(AttachAction(cameraLabel, cameraIcon, onCamera)) + actions"
            )
        )
    }

    @Test
    fun compactItemsLeaveTheNextChoicePeekingOnScreen() {
        assertEquals(104f, AttachmentCarouselMetrics.ITEM_WIDTH_DP, epsilon)
        assertEquals(96f, AttachmentCarouselMetrics.ITEM_HEIGHT_DP, epsilon)
        assertEquals(8f, AttachmentCarouselMetrics.ITEM_SPACING_DP, epsilon)

        val viewport = 412f - AttachmentCarouselMetrics.OUTER_HORIZONTAL_PADDING_DP * 2 -
            AttachmentCarouselMetrics.HORIZONTAL_PADDING_DP * 2
        val visibleItems = (viewport + AttachmentCarouselMetrics.ITEM_SPACING_DP) /
            (AttachmentCarouselMetrics.ITEM_WIDTH_DP + AttachmentCarouselMetrics.ITEM_SPACING_DP)
        assertTrue(visibleItems > 3f)
        assertTrue(visibleItems < 4f)
    }

    @Test
    fun carouselKeepsAllLabelsOnOneLine() {
        assertTrue(source.contains("maxLines = 1"))
        assertTrue(source.contains("overflow = TextOverflow.Ellipsis"))
    }
}
