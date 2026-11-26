package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentedListsTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun listEndsAndMiddleResolveToTheExpectedShapes() {
        assertEquals(RowPosition.SINGLE, segmentedListPosition(0, 1))
        assertEquals(RowPosition.FIRST, segmentedListPosition(0, 4))
        assertEquals(RowPosition.MIDDLE, segmentedListPosition(1, 4))
        assertEquals(RowPosition.MIDDLE, segmentedListPosition(2, 4))
        assertEquals(RowPosition.LAST, segmentedListPosition(3, 4))
    }

    @Test
    fun contactPickerUsesSegmentedMaterialRows() {
        val contacts = source("java/com/anindra/messages/ui/NewChatScreen.kt")

        assertTrue(contacts.contains("itemsIndexed(filtered"))
        assertTrue(contacts.contains("segmentedListPosition(index, filtered.size)"))
        assertTrue(contacts.contains("shape = segmentedListShape(position)"))
        assertTrue(contacts.contains("tint = MaterialTheme.colorScheme.surfaceContainerLow"))
        assertTrue(contacts.contains("TranslucentBackdropSurface("))
        assertTrue(contacts.contains("kind=contact position=\$position"))
    }

    @Test
    fun inboxRowsAndSwipeBackgroundShareTheSegmentShape() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val components = source("java/com/anindra/messages/ui/Components.kt")

        assertTrue(conversations.contains("itemsIndexed(scoped"))
        assertTrue(conversations.contains("segmentedListPosition(index, scoped.size)"))
        assertTrue(conversations.contains("modifier.clip(segmentedListShape(position))"))
        assertTrue(conversations.contains(".clip(segmentedListShape(position))"))
        assertTrue(conversations.contains("MaterialTheme.colorScheme.surfaceContainerLow"))
        assertTrue(conversations.contains("kind=conversation position=\$position"))
        assertTrue(components.contains("SkeletonConversationRow(position:"))
        assertTrue(components.contains(".clip(segmentedListShape(position))"))
    }

    @Test
    fun longPressOptionsAreOneDynamicSegmentedList() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val sheet = conversations.substringAfter("if (sheetConvo != null)")
            .substringBefore("permanentDeleteTarget?.let")

        assertTrue(sheet.contains("val sheetActions = buildList"))
        assertTrue(sheet.contains("sheetActions.forEachIndexed"))
        assertTrue(sheet.contains("segmentedListPosition(index, sheetActions.size)"))
        assertTrue(conversations.contains("fun SheetActionRow("))
        assertTrue(conversations.contains("MaterialTheme.colorScheme.surfaceContainerHigh.copy("))
        assertTrue(conversations.contains("alpha = LocalTranslucentSurfaceAlpha.current"))
        assertTrue(conversations.contains("kind=action position=\$position"))
    }
}
