package com.anindra.messages.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstLaunchSetupScreenTest {
    private val root by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "app/src/main").isDirectory }
            ?: error("app/src/main not found")
    }
    private val source by lazy {
        File(root, "app/src/main/java/com/anindra/messages/ui/FirstLaunchSetupScreen.kt").readText()
    }
    private val activity by lazy {
        File(root, "app/src/main/java/com/anindra/messages/MainActivity.kt").readText()
    }

    @Test
    fun setupRequestsRoleThenPermissionsThenCompletes() {
        assertEquals(
            FirstLaunchAction.REQUEST_DEFAULT_SMS,
            FirstLaunchSetupFlow.nextAction(defaultSmsReady = false, permissionsReady = false)
        )
        assertEquals(
            FirstLaunchAction.REQUEST_PERMISSIONS,
            FirstLaunchSetupFlow.nextAction(defaultSmsReady = true, permissionsReady = false)
        )
        assertEquals(
            FirstLaunchAction.COMPLETE,
            FirstLaunchSetupFlow.nextAction(defaultSmsReady = true, permissionsReady = true)
        )
    }

    @Test
    fun backgroundUsesFourEqualRotatingExpressiveShapesAndNoMovingGem() {
        assertTrue(source.contains("MaterialShapes.Cookie12Sided.toShape()"))
        assertTrue(source.contains("MaterialShapes.Clover8Leaf.toShape()"))
        assertTrue(source.contains("MaterialShapes.Cookie6Sided.toShape()"))
        assertTrue(source.contains("MaterialShapes.Cookie9Sided.toShape()"))
        assertTrue(!source.contains("MaterialShapes.Gem.toShape()"))
        assertEquals(4, Regex("\\.size\\(shapeSize\\)").findAll(source).count())
        assertTrue(source.contains("durationMillis = 14_000"))
        assertTrue(source.contains("rotationProgress.value * 360f"))
        assertTrue(source.contains("rotationProgress.value * 300f"))
        assertTrue(source.contains("rotationProgress.value * 420f"))
        assertTrue(source.contains("R.string.onboarding_hero_title"))
    }

    @Test
    fun reducedMotionStopsAllFourShapeRotations() {
        assertTrue(source.contains("if (reduceMotion)"))
        assertTrue(source.contains("rotationProgress.snapTo(0f)"))
        assertTrue(source.contains("val reduceMotion = LocalReduceMotion.current"))
    }

    @Test
    fun collapsedActionsExpandIntoADraggableNinetyPercentGlassSheet() {
        assertTrue(source.contains("screenHeight * 0.10f"))
        assertTrue(source.contains("ChatSheetLayout.HEIGHT_FRACTION"))
        assertTrue(source.contains("BottomSheetScaffold("))
        assertTrue(source.contains("setupStarted = true"))
        assertTrue(source.contains("bottomSheetState.expand()"))
        assertTrue(source.contains("sheetSwipeEnabled = setupStarted"))
        assertTrue(source.contains("BottomSheetDefaults.DragHandle()"))
        assertTrue(source.contains("R.string.onboarding_start_setup"))
        assertTrue(source.contains("R.string.onboarding_skip_setup"))
        assertTrue(source.contains("OutlinedButton("))
        assertTrue(source.contains("ChatSheetLayout.BACKGROUND_BLUR_RADIUS"))
        assertTrue(source.contains("alpha = surfaceAlpha"))
    }

    @Test
    fun activityGatesTheAppUntilSetupIsFinished() {
        assertTrue(activity.contains("vm.settings.onboardingCompleted"))
        assertTrue(activity.contains("FirstLaunchSetupScreen("))
        assertTrue(activity.contains("requestDefaultSmsRole()"))
        assertTrue(activity.contains("requestMissingSmsPermissions()"))
        assertTrue(activity.contains("return@MessagesTheme"))
    }

    @Test
    fun checklistCoversDefaultSmsPermissionsAndPrivacy() {
        assertTrue(source.contains("R.string.onboarding_default_title"))
        assertTrue(source.contains("R.string.onboarding_permissions_title"))
        assertTrue(source.contains("R.string.onboarding_privacy"))
        assertTrue(source.contains("onRequestDefaultSms"))
        assertTrue(source.contains("onRequestPermissions"))
        assertTrue(source.contains("onNotNow"))
    }

    @Test
    fun onboardingCopyIsAvailableInEverySupportedLocale() {
        val resourceRoot = File(root, "app/src/main/res")
        val files = resourceRoot.listFiles()
            .orEmpty()
            .filter { it.name == "values" || it.name.startsWith("values-") }
            .map { File(it, "strings_onboarding.xml") }
            .filter { it.isFile }
        assertEquals(14, files.size)
        val required = listOf(
            "onboarding_hero_title",
            "onboarding_title",
            "onboarding_default_title",
            "onboarding_permissions_title",
            "onboarding_start_setup",
            "onboarding_skip_setup",
            "onboarding_action_finish",
            "onboarding_not_now"
        )
        files.forEach { file ->
            val copy = file.readText()
            required.forEach { key ->
                assertTrue("$key missing from $file", copy.contains("name=\"$key\""))
            }
            assertTrue(
                "hero title changed in $file",
                copy.contains(">Messagerie M3 Expressive</string>")
            )
        }
    }
}
