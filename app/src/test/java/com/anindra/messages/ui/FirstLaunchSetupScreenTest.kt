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
        assertTrue(source.contains("MaterialShapes.Cookie12Sided.toShape()"))
        assertTrue(source.contains("MaterialShapes.Clover8Leaf.toShape()"))
        assertTrue(source.contains("MaterialShapes.Cookie6Sided.toShape()"))
        assertTrue(source.contains("MaterialShapes.Cookie9Sided.toShape()"))
        assertTrue(source.contains("MaterialShapes.Gem.toShape()"))
        assertTrue(source.contains("SetupShapeMotion.frame(motionProgress.value)"))
        assertTrue(source.contains("durationMillis = 4_200"))
        assertTrue(source.contains("val reduceMotion = LocalReduceMotion.current"))
        assertTrue(source.contains("SetupShapeMotion.resting"))
    }

    @Test
    fun fourImpactsSendTheMovingShapeUpInAZigzag() {
        val start = SetupShapeMotion.frame(0f)
        val lower = SetupShapeMotion.frame(0.18f)
        val middle = SetupShapeMotion.frame(0.40f)
        val upperLeft = SetupShapeMotion.frame(0.62f)
        val upperRight = SetupShapeMotion.frame(0.82f)
        val end = SetupShapeMotion.frame(1f)

        assertTrue("moving shape must enter from below", start.movingYFraction > 1f)
        assertTrue("first collision must travel left", lower.movingXFraction < start.movingXFraction)
        assertTrue("second collision must rebound right", middle.movingXFraction > lower.movingXFraction)
        assertTrue("third collision must rebound left", upperLeft.movingXFraction < middle.movingXFraction)
        assertTrue("fourth collision must rebound right", upperRight.movingXFraction > upperLeft.movingXFraction)
        assertTrue("final rebound must leave to the left", end.movingXFraction < upperRight.movingXFraction)
        assertTrue("moving shape must leave above the screen", end.movingYFraction < 0f)
        assertTrue("lower obstacle must receive the first impact", lower.lowerImpact > 0.99f)
        assertTrue("middle obstacle must receive the second impact", middle.middleImpact > 0.99f)
        assertTrue("upper-left obstacle must receive the third impact", upperLeft.upperLeftImpact > 0.99f)
        assertTrue("upper-right obstacle must receive the fourth impact", upperRight.upperRightImpact > 0.99f)
        assertTrue("moving shape must squash on impact", middle.movingScaleY < 1f)
    }

    @Test
    fun reducedMotionKeepsAReadableCollisionFreeComposition() {
        val frame = SetupShapeMotion.resting
        assertEquals(1f, frame.movingScaleX, 0f)
        assertEquals(1f, frame.movingScaleY, 0f)
        assertEquals(0f, frame.lowerImpact, 0f)
        assertEquals(0f, frame.middleImpact, 0f)
        assertEquals(0f, frame.upperLeftImpact, 0f)
        assertEquals(0f, frame.upperRightImpact, 0f)
        assertTrue(frame.movingYFraction in 0f..0.5f)
    }

    @Test
    fun collapsedActionsExpandARealNinetyPercentSetupSheet() {
        assertTrue(source.contains("screenHeight * 0.10f"))
        assertTrue(source.contains("screenHeight * 0.90f"))
        assertTrue(source.contains("setupExpanded = true"))
        assertTrue(source.contains("R.string.onboarding_start_setup"))
        assertTrue(source.contains("R.string.onboarding_skip_setup"))
        assertTrue(source.contains("OutlinedButton("))
        assertTrue(source.contains("TranslucentBackdropSurface("))
        assertTrue(source.contains("blurRadius = 32.dp"))
        assertTrue(source.contains("tintAlpha = 0.72f"))
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
            "onboarding_start_setup",
            "onboarding_skip_setup",
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
