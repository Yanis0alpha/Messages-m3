package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarExpressiveShapeTest {
    private val components = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main/java/com/anindra/messages/ui/Components.kt") }
        .firstOrNull { it.isFile }
        ?.readText()
        ?: error("Components.kt not found")

    @Test
    fun pseudoRandomShapeIsStableAndVaried() {
        val keys = listOf("Alice", "Bob", "+2135550101", "Family", "Work", "OTP")
        keys.forEach { key ->
            assertEquals(avatarShapeIndex(key, 7), avatarShapeIndex(key, 7))
            assertTrue(avatarShapeIndex(key, 7) in 0..6)
        }
        assertTrue(keys.map { avatarShapeIndex(it, 7) }.toSet().size > 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shapeSelectionRejectsAnEmptyPalette() {
        avatarShapeIndex("Alice", 0)
    }

    @Test
    fun contactPhotosAndFallbacksUseTheSameExpressiveShape() {
        listOf(
            "MaterialShapes.Gem.toShape()",
            "MaterialShapes.Cookie6Sided.toShape()",
            "MaterialShapes.Cookie7Sided.toShape()",
            "MaterialShapes.Cookie9Sided.toShape()",
            "MaterialShapes.Cookie12Sided.toShape()",
            "MaterialShapes.Clover8Leaf.toShape()",
            "MaterialShapes.Sunny.toShape()"
        ).forEach { assertTrue("missing expressive avatar shape: $it", components.contains(it)) }

        assertTrue(components.contains("val generatedShape = expressiveAvatarShape(key)"))
        assertTrue(components.contains(".background(fill, avatarShape)"))
        assertTrue(components.contains(".clip(avatarShape)"))
        assertTrue(components.contains("modifier = modifier.size(size).clip(ownerShape)"))
        assertTrue(components.contains("shape = ownerShape"))
    }
}
