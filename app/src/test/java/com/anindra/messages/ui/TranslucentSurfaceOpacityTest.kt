package com.anindra.messages.ui

import com.anindra.messages.data.TranslucentSurfaceOpacity
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslucentSurfaceOpacityTest {
    private val main = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main") }
        .firstOrNull { it.isDirectory }
        ?: error("app/src/main not found")

    private fun source(path: String) = File(main, path).readText()

    @Test
    fun scaleStaysReadableAndSnapsToFivePercentSteps() {
        assertEquals(30, TranslucentSurfaceOpacity.MIN_PERCENT)
        assertEquals(95, TranslucentSurfaceOpacity.MAX_PERCENT)
        assertEquals(60, TranslucentSurfaceOpacity.DEFAULT_PERCENT)
        assertEquals(30, TranslucentSurfaceOpacity.fromSlider(28f))
        assertEquals(65, TranslucentSurfaceOpacity.fromSlider(63f))
        assertEquals(95, TranslucentSurfaceOpacity.fromSlider(100f))
        assertEquals(0.6f, TranslucentSurfaceOpacity.defaultAlpha, 0f)
    }

    @Test
    fun settingsSliderPersistsEveryVisibleStep() {
        val settings = source("java/com/anindra/messages/ui/SettingsScreen.kt")
        val store = source("java/com/anindra/messages/data/SettingsStore.kt")

        assertTrue(settings.contains("settings_translucent_surface_opacity_title"))
        assertTrue(settings.contains("Slider("))
        assertTrue(settings.contains("TranslucentSurfaceOpacity.fromSlider(value)"))
        assertTrue(settings.contains("vm.settings.translucentSurfaceOpacityPercent = percent"))
        assertTrue(settings.contains("stateDescription = opacityValue"))
        assertTrue(store.contains("KEY_TRANSLUCENT_SURFACE_OPACITY"))
        assertTrue(store.contains("var translucentSurfaceOpacityPercent: Int"))
        assertTrue(store.contains("_revision.value++"))
    }

    @Test
    fun oneCompositionValueFeedsEveryBlurredSurface() {
        val activity = source("java/com/anindra/messages/MainActivity.kt")
        val navigation = source("java/com/anindra/messages/ui/MessagesNavigationBar.kt")
        val chatSheet = source("java/com/anindra/messages/ui/ConversationChatSheet.kt")
        val conversations = source("java/com/anindra/messages/ui/ConversationsScreen.kt")

        assertTrue(activity.contains("TranslucentSurfaceOpacity.alpha("))
        assertTrue(
            activity.contains(
                "LocalTranslucentSurfaceAlpha provides translucentSurfaceAlpha"
            )
        )
        assertTrue(navigation.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(navigation.contains("alpha = surfaceAlpha"))
        assertTrue(chatSheet.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(chatSheet.contains("alpha = surfaceAlpha"))
        assertTrue(conversations.contains("LocalTranslucentSurfaceAlpha.current"))
        assertTrue(conversations.contains("alpha = translucentSurfaceAlpha"))
    }
}
