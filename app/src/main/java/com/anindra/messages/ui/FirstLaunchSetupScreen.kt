package com.anindra.messages.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anindra.messages.R
import com.anindra.messages.ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin

internal enum class FirstLaunchAction {
    REQUEST_DEFAULT_SMS,
    REQUEST_PERMISSIONS,
    COMPLETE
}

internal object FirstLaunchSetupFlow {
    fun nextAction(defaultSmsReady: Boolean, permissionsReady: Boolean): FirstLaunchAction = when {
        !defaultSmsReady -> FirstLaunchAction.REQUEST_DEFAULT_SMS
        !permissionsReady -> FirstLaunchAction.REQUEST_PERMISSIONS
        else -> FirstLaunchAction.COMPLETE
    }
}

internal data class SetupShapeMotionFrame(
    val cookieXFraction: Float,
    val cookieYFraction: Float,
    val cookieRotation: Float,
    val cookieScaleX: Float,
    val cookieScaleY: Float,
    val gemXFraction: Float,
    val gemYFraction: Float,
    val gemRotation: Float,
    val gemScaleX: Float,
    val gemScaleY: Float
)

/**
 * One deterministic pass of the setup artwork. The timing follows the same model used by
 * Feldman Clock's open-source Setup Wizard artwork (`feldmandev/feldman-clock`): gravity
 * accelerates the falling body, restitution sends it back up, and the large cookie behaves as a
 * springy trampoline at the collision. The equations here are a small local implementation, so
 * this screen does not pull the reference app's much larger motion-and-font dependency.
 */
internal object SetupShapeMotion {
    private const val IMPACT_START = 0.48f
    private const val IMPACT_END = 0.58f

    val resting = SetupShapeMotionFrame(
        cookieXFraction = 0f,
        cookieYFraction = 0f,
        cookieRotation = -12f,
        cookieScaleX = 1f,
        cookieScaleY = 1f,
        gemXFraction = 0.53f,
        gemYFraction = 0.34f,
        gemRotation = 58f,
        gemScaleX = 1f,
        gemScaleY = 1f
    )

    fun frame(rawProgress: Float): SetupShapeMotionFrame {
        val progress = rawProgress.coerceIn(0f, 1f)
        val entrance = phase(progress, 0f, 0.20f)
        val entranceSpring = easeOutBack(entrance)
        val exit = phase(progress, 0.84f, 1f)
        val impact = phase(progress, IMPACT_START, IMPACT_END)
        val compression = if (progress in IMPACT_START..IMPACT_END) {
            sin(impact * PI.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val gemX: Float
        val gemY: Float
        val gemRotation: Float
        val gemScaleX: Float
        val gemScaleY: Float
        when {
            progress < 0.16f -> {
                gemX = 0.70f
                gemY = -0.34f
                gemRotation = -28f
                gemScaleX = 1f
                gemScaleY = 1f
            }
            progress < IMPACT_START -> {
                val fall = phase(progress, 0.16f, IMPACT_START)
                // Constant gravity: travelled distance is proportional to time squared.
                val gravity = fall * fall
                gemX = lerp(0.70f, 0.52f, fall)
                gemY = lerp(-0.34f, 0.50f, gravity)
                gemRotation = lerp(-28f, 68f, fall)
                gemScaleX = 1f
                gemScaleY = 1f
            }
            progress <= IMPACT_END -> {
                gemX = 0.52f
                gemY = 0.50f + 0.026f * compression
                gemRotation = lerp(68f, 94f, impact)
                // Squash on contact, then stretch as restitution takes over.
                val release = phase(impact, 0.5f, 1f)
                if (impact < 0.5f) {
                    val squash = sin(impact * PI.toFloat())
                    gemScaleX = 1f + 0.13f * squash
                    gemScaleY = 1f - 0.20f * squash
                } else {
                    gemScaleX = lerp(1.13f, 0.934f, release)
                    gemScaleY = lerp(0.80f, 1.12f, release)
                }
            }
            progress < 0.84f -> {
                val rebound = phase(progress, IMPACT_END, 0.84f)
                // Restitution gives a high initial velocity which eases as the gem reaches its apex.
                val ballisticRise = 1f - (1f - rebound) * (1f - rebound)
                gemX = lerp(0.52f, 0.61f, rebound)
                gemY = lerp(0.50f, -0.34f, ballisticRise)
                gemRotation = lerp(94f, 178f, rebound)
                val stretch = (1f - rebound) * 0.12f
                gemScaleX = 1f - stretch * 0.55f
                gemScaleY = 1f + stretch
            }
            else -> {
                gemX = 0.61f
                gemY = -0.34f
                gemRotation = 178f
                gemScaleX = 1f
                gemScaleY = 1f
            }
        }

        return SetupShapeMotionFrame(
            cookieXFraction = -0.10f * (1f - entrance.coerceIn(0f, 1f)) - 0.10f * exit,
            cookieYFraction = 0.74f * (1f - entranceSpring) + 0.74f * exit,
            cookieRotation = lerp(-24f, -12f, entrance.coerceIn(0f, 1f)) +
                2.5f * compression,
            cookieScaleX = 1f + 0.075f * compression,
            cookieScaleY = 1f - 0.095f * compression,
            gemXFraction = gemX,
            gemYFraction = gemY,
            gemRotation = gemRotation,
            gemScaleX = gemScaleX,
            gemScaleY = gemScaleY
        )
    }

    private fun phase(value: Float, start: Float, end: Float): Float =
        ((value - start) / (end - start)).coerceIn(0f, 1f)

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction

    /** Slight overshoot gives the heavy cookie a spring rather than a tweened slide. */
    private fun easeOutBack(value: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val shifted = value - 1f
        return 1f + c3 * shifted * shifted * shifted + c1 * shifted * shifted
    }
}

/** Pixel-inspired first-run setup with a bouncing expressive gem. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FirstLaunchSetupScreen(
    defaultSmsReady: Boolean,
    permissionsReady: Boolean,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onComplete: () -> Unit,
    onNotNow: () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val action = FirstLaunchSetupFlow.nextAction(defaultSmsReady, permissionsReady)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val visualHeight = maxHeight * 0.43f
            val cookieSize = if (maxWidth < 520.dp) maxWidth * 0.94f else 460.dp
            val motionProgress = remember { Animatable(0f) }

            LaunchedEffect(reduceMotion) {
                if (reduceMotion) {
                    motionProgress.snapTo(0f)
                } else {
                    while (true) {
                        motionProgress.snapTo(0f)
                        motionProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 3_000,
                                easing = LinearEasing
                            )
                        )
                    }
                }
            }
            val motionFrame = if (reduceMotion) {
                SetupShapeMotion.resting
            } else {
                SetupShapeMotion.frame(motionProgress.value)
            }

            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(visualHeight)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_brand),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 24.dp, top = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(
                                x = -(cookieSize * 0.28f) +
                                    cookieSize * motionFrame.cookieXFraction,
                                y = cookieSize * 0.30f +
                                    cookieSize * motionFrame.cookieYFraction
                            )
                            .size(cookieSize)
                            .graphicsLayer {
                                rotationZ = motionFrame.cookieRotation
                                scaleX = motionFrame.cookieScaleX
                                scaleY = motionFrame.cookieScaleY
                                transformOrigin = TransformOrigin(0.5f, 0.82f)
                            }
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = maxWidth * motionFrame.gemXFraction,
                                y = visualHeight * motionFrame.gemYFraction
                            )
                            .size(96.dp)
                            .graphicsLayer {
                                rotationZ = motionFrame.gemRotation
                                scaleX = motionFrame.gemScaleX
                                scaleY = motionFrame.gemScaleY
                            }
                            .clip(MaterialShapes.Gem.toShape())
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    SetupRequirementCard(
                        icon = Icons.Rounded.Sms,
                        title = stringResource(R.string.onboarding_default_title),
                        subtitle = stringResource(R.string.onboarding_default_subtitle),
                        complete = defaultSmsReady,
                        onClick = onRequestDefaultSms
                    )
                    Spacer(Modifier.height(10.dp))
                    SetupRequirementCard(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.onboarding_permissions_title),
                        subtitle = stringResource(R.string.onboarding_permissions_subtitle),
                        complete = permissionsReady,
                        onClick = onRequestPermissions
                    )
                    Spacer(Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.onboarding_privacy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(22.dp))

                    Button(
                        onClick = {
                            when (action) {
                                FirstLaunchAction.REQUEST_DEFAULT_SMS -> onRequestDefaultSms()
                                FirstLaunchAction.REQUEST_PERMISSIONS -> onRequestPermissions()
                                FirstLaunchAction.COMPLETE -> onComplete()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                when (action) {
                                    FirstLaunchAction.REQUEST_DEFAULT_SMS ->
                                        R.string.onboarding_action_default
                                    FirstLaunchAction.REQUEST_PERMISSIONS ->
                                        R.string.onboarding_action_permissions
                                    FirstLaunchAction.COMPLETE ->
                                        R.string.onboarding_action_finish
                                }
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = if (action == FirstLaunchAction.COMPLETE) {
                                Icons.Rounded.Check
                            } else {
                                Icons.AutoMirrored.Rounded.ArrowForward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    TextButton(onClick = onNotNow) {
                        Text(stringResource(R.string.onboarding_not_now))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SetupRequirementCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    complete: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (complete) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(enabled = !complete, onClick = onClick),
        color = container,
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialShapes.Cookie6Sided.toShape())
                    .background(
                        if (complete) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (complete) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (complete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            ) {
                if (complete) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.onboarding_ready),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(6.dp).size(18.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_pending),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
