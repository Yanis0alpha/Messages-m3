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
    fun pixelInspiredArtworkUsesTheRequestedExpressiveMotion() {
        assertTrue(source.contains("MaterialShapes.Cookie9Sided.toShape()"))
        assertTrue(source.contains("MaterialShapes.Gem.toShape()"))
        assertTrue(source.contains("SetupShapeMotion.frame(motionProgress.value)"))
        assertTrue(source.contains("durationMillis = 3_000"))
        assertTrue(source.contains("val reduceMotion = LocalReduceMotion.current"))
        assertTrue(source.contains("SetupShapeMotion.resting"))
    }

    @Test
    fun gravityImpactAndRestitutionAreOneCoordinatedMotion() {
        val start = SetupShapeMotion.frame(0f)
        val fallingEarly = SetupShapeMotion.frame(0.24f)
        val fallingMiddle = SetupShapeMotion.frame(0.32f)
        val fallingLate = SetupShapeMotion.frame(0.40f)
        val impact = SetupShapeMotion.frame(0.53f)
        val rebound = SetupShapeMotion.frame(0.68f)
        val end = SetupShapeMotion.frame(1f)

        assertTrue("cookie must enter from below-left", start.cookieYFraction > 0.4f)
        assertTrue(
            "falling distance must accelerate under gravity",
            fallingLate.gemYFraction - fallingMiddle.gemYFraction >
                fallingMiddle.gemYFraction - fallingEarly.gemYFraction
        )
        assertTrue("cookie must squash at impact", impact.cookieScaleY < 1f)
        assertTrue("cookie must spread at impact", impact.cookieScaleX > 1f)
        assertTrue("gem must squash at impact", impact.gemScaleY < 1f)
        assertTrue("restitution must send the gem upward", rebound.gemYFraction < impact.gemYFraction)
        assertTrue("cookie must leave below before the loop restarts", end.cookieYFraction > 0.4f)
    }

    @Test
    fun reducedMotionKeepsAReadableCollisionFreeComposition() {
        val frame = SetupShapeMotion.resting
        assertEquals(1f, frame.cookieScaleX, 0f)
        assertEquals(1f, frame.cookieScaleY, 0f)
        assertEquals(1f, frame.gemScaleX, 0f)
        assertEquals(1f, frame.gemScaleY, 0f)
        assertTrue(frame.gemYFraction < 0.5f)
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
            "onboarding_title",
            "onboarding_default_title",
            "onboarding_permissions_title",
            "onboarding_action_finish",
            "onboarding_not_now"
        )
        files.forEach { file ->
            required.forEach { key ->
                assertTrue("$key missing from $file", file.readText().contains("name=\"$key\""))
            }
        }
    }
}
