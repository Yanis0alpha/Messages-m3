package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeExpressiveShapeTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private val conversations by lazy {
        File(main, "java/com/anindra/messages/ui/ConversationsScreen.kt").readText()
    }

    @Test
    fun liveSwipeRollsAContainerWithTheConfiguredActionIconInside() {
        val swipe = conversations.substringAfter("internal fun SwipeConversationItem(")
            .substringBefore("private fun SwipeAction.background()")

        assertTrue(swipe.contains("dismissState.requireOffset()"))
        assertTrue(swipe.contains("swipeWidthPx"))
        assertTrue(swipe.contains("rollingSwipeShapeTranslation("))
        assertTrue(swipe.contains("rollingSwipeShapeRotation("))
        assertTrue(swipe.contains(".background(actionColor, action.expressiveShape())"))
        assertTrue(swipe.contains("imageVector = action.icon("))
        assertTrue(swipe.contains("modifier = Modifier.size(SettingsLayout.SWIPE_ICON_SIZE)"))
        assertTrue(swipe.contains("val shapeProgress = if (reduceMotion) 1f else gestureProgress"))
    }

    @Test
    fun everyConfiguredActionHasAnExpressiveMaterialShape() {
        val shapes = conversations.substringAfter("private fun SwipeAction.expressiveShape()")
            .substringBefore("fun SwipeActionPreview(")

        assertTrue(shapes.contains("SwipeAction.ARCHIVE -> MaterialShapes.Cookie7Sided"))
        assertTrue(shapes.contains("SwipeAction.DELETE -> MaterialShapes.Gem"))
        assertTrue(shapes.contains("SwipeAction.MARK_READ_UNREAD -> MaterialShapes.Cookie9Sided"))
        assertTrue(shapes.contains("SwipeAction.PIN -> MaterialShapes.Clover8Leaf"))
        assertTrue(shapes.contains("SwipeAction.BLOCK -> MaterialShapes.Sunny"))
    }

    @Test
    fun settingsPreviewUsesTheSameShapeAndIconPair() {
        val preview = conversations.substringAfter("private fun RowScope.IconBlock(")
            .substringBefore("fun SwipeAction.labelRes(")

        assertTrue(preview.contains("action.expressiveShape()"))
        assertTrue(preview.contains("action.icon(unread = false)"))
    }
}
