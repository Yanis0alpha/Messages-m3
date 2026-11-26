package com.anindra.messages.ui

import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxCategoryButtonGroupTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun connectedGroupUsesSmallGapsAndMorphsSelectionToAPill() {
        assertEquals(40.dp, InboxCategoryButtonGroupLayout.HEIGHT)
        assertEquals(48.dp, InboxCategoryButtonGroupLayout.MIN_WIDTH)
        assertEquals(2.dp, InboxCategoryButtonGroupLayout.GAP)
        assertEquals(20.dp, InboxCategoryButtonGroupLayout.startCorner(0, 4, false))
        assertEquals(8.dp, InboxCategoryButtonGroupLayout.endCorner(0, 4, false))
        assertEquals(8.dp, InboxCategoryButtonGroupLayout.startCorner(1, 4, false))
        assertEquals(20.dp, InboxCategoryButtonGroupLayout.startCorner(1, 4, true))
        assertEquals(20.dp, InboxCategoryButtonGroupLayout.endCorner(1, 4, true))

        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val group = conversations.substringAfter("internal fun InboxCategoryChips(")
            .substringBefore("internal val InboxCategory.labelRes")
        val button = conversations.substringAfter("internal fun InboxCategoryChip(")
            .substringBefore("internal fun SearchScopeChips(")

        assertTrue(group.contains("Arrangement.spacedBy(InboxCategoryButtonGroupLayout.GAP)"))
        assertTrue(group.contains(".selectableGroup()"))
        assertTrue(group.contains("add(null to stringResource(R.string.inbox_category_all))"))
        assertTrue(group.contains("InboxCategory.entries.forEach"))
        assertTrue(group.contains("if (isSelected && category != null) null else category"))
        assertTrue(button.contains("animateDpAsState("))
        assertTrue(button.contains("Surface("))
        assertTrue(button.contains("selected = selected"))
        assertTrue(button.contains("Role.RadioButton"))
        assertTrue(button.contains("colors.secondaryContainer"))
        assertTrue(button.contains("colors.surfaceContainerLow"))
        assertFalse(button.contains("FilterChip("))
        assertFalse(button.contains("BorderStroke("))
        assertFalse(button.contains(".weight("))
    }

    @Test
    fun emptyArchiveUsesAnExpressiveMaterialState() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        assertTrue(conversations.contains("showArchived && !searching && scoped.isEmpty()"))
        assertTrue(conversations.contains("ArchivedEmptyState("))
        assertTrue(conversations.contains("MaterialShapes.Cookie7Sided.toShape()"))
        assertTrue(conversations.contains("Icons.Rounded.Archive"))
        assertTrue(conversations.contains("R.string.conversations_archived_empty"))
    }
}
