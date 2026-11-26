package com.anindra.messages.ui

import java.io.File
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
    fun inboxCategoriesUseAConnectedSingleChoiceGroup() {
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")
        val group = conversations.substringAfter("internal fun InboxCategoryChips(")
            .substringBefore("internal val InboxCategory.labelRes")
        val button = conversations.substringAfter("internal fun InboxCategoryChip(")
            .substringBefore("internal fun SearchScopeChips(")

        assertTrue(group.contains("SingleChoiceSegmentedButtonRow("))
        assertTrue(group.contains("SegmentedButtonDefaults.itemShape(index, options.size)"))
        assertTrue(group.contains("add(null to stringResource(R.string.inbox_category_all))"))
        assertTrue(group.contains("InboxCategory.entries.forEach"))
        assertTrue(button.contains("Surface("))
        assertTrue(button.contains("selected = selected"))
        assertTrue(button.contains("Role.RadioButton"))
        assertTrue(button.contains("colors.secondaryContainer"))
        assertTrue(button.contains("colors.surfaceContainerLow"))
        assertFalse(button.contains("FilterChip("))
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
