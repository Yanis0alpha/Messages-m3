package com.anindra.messages.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val movingXFraction: Float,
    val movingYFraction: Float,
    val movingRotation: Float,
    val movingScaleX: Float,
    val movingScaleY: Float,
    val lowerImpact: Float,
    val middleImpact: Float,
    val upperLeftImpact: Float,
    val upperRightImpact: Float
)

/**
 * A lightweight multi-collision physics timeline inspired by the open-source Feldman Clock setup
 * artwork. The moving shape is launched upward, loses speed under gravity, collides with four
 * staggered Material shapes and leaves through the upper-left in a natural zigzag. Each impact is shared
 * with the obstacle so the background visibly absorbs and returns the energy.
 */
internal object SetupShapeMotion {
    private const val LOWER_HIT = 0.18f
    private const val MIDDLE_HIT = 0.40f
    private const val UPPER_LEFT_HIT = 0.62f
    private const val UPPER_RIGHT_HIT = 0.82f

    val resting = SetupShapeMotionFrame(
        movingXFraction = 0.52f,
        movingYFraction = 0.16f,
        movingRotation = 24f,
        movingScaleX = 1f,
        movingScaleY = 1f,
        lowerImpact = 0f,
        middleImpact = 0f,
        upperLeftImpact = 0f,
        upperRightImpact = 0f
    )

    fun frame(rawProgress: Float): SetupShapeMotionFrame {
        val progress = rawProgress.coerceIn(0f, 1f)
        val lowerImpact = impactPulse(progress, LOWER_HIT)
        val middleImpact = impactPulse(progress, MIDDLE_HIT)
        val upperLeftImpact = impactPulse(progress, UPPER_LEFT_HIT)
        val upperRightImpact = impactPulse(progress, UPPER_RIGHT_HIT)
        val strongestImpact = maxOf(
            lowerImpact,
            middleImpact,
            upperLeftImpact,
            upperRightImpact
        )

        val point = when {
            progress <= LOWER_HIT -> arcPoint(
                phase(progress, 0f, LOWER_HIT),
                fromX = 0.38f,
                fromY = 1.08f,
                toX = 0.22f,
                toY = 0.70f,
                gravityArc = 0.045f
            )
            progress <= MIDDLE_HIT -> arcPoint(
                phase(progress, LOWER_HIT, MIDDLE_HIT),
                fromX = 0.22f,
                fromY = 0.70f,
                toX = 0.58f,
                toY = 0.56f,
                gravityArc = 0.055f
            )
            progress <= UPPER_LEFT_HIT -> arcPoint(
                phase(progress, MIDDLE_HIT, UPPER_LEFT_HIT),
                fromX = 0.58f,
                fromY = 0.56f,
                toX = 0.20f,
                toY = 0.36f,
                gravityArc = 0.050f
            )
            progress <= UPPER_RIGHT_HIT -> arcPoint(
                phase(progress, UPPER_LEFT_HIT, UPPER_RIGHT_HIT),
                fromX = 0.20f,
                fromY = 0.36f,
                toX = 0.66f,
                toY = 0.17f,
                gravityArc = 0.045f
            )
            else -> arcPoint(
                phase(progress, UPPER_RIGHT_HIT, 1f),
                fromX = 0.66f,
                fromY = 0.17f,
                toX = -0.20f,
                toY = -0.14f,
                gravityArc = 0.035f
            )
        }

        return SetupShapeMotionFrame(
            movingXFraction = point.first,
            movingYFraction = point.second,
            movingRotation = -36f + progress * 520f,
            movingScaleX = 1f + strongestImpact * 0.16f,
            movingScaleY = 1f - strongestImpact * 0.20f,
            lowerImpact = lowerImpact,
            middleImpact = middleImpact,
            upperLeftImpact = upperLeftImpact,
            upperRightImpact = upperRightImpact
        )
    }

    private fun arcPoint(
        fraction: Float,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        gravityArc: Float
    ): Pair<Float, Float> {
        // A fresh rebound starts quickly and decelerates as gravity wins near the next obstacle.
        val travelled = 1f - (1f - fraction) * (1f - fraction)
        val gravity = 4f * fraction * (1f - fraction) * gravityArc
        return lerp(fromX, toX, travelled) to lerp(fromY, toY, travelled) + gravity
    }

    private fun impactPulse(progress: Float, center: Float): Float =
        (1f - kotlin.math.abs(progress - center) / 0.045f).coerceIn(0f, 1f)

    private fun phase(value: Float, start: Float, end: Float): Float =
        ((value - start) / (end - start)).coerceIn(0f, 1f)

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction
}

/** Pixel-inspired first-run setup with a four-collision expressive shape path. */
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
    var setupExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val proposedCollapsedHeight = screenHeight * 0.10f
            val collapsedHeight = if (proposedCollapsedHeight < 80.dp) {
                80.dp
            } else {
                proposedCollapsedHeight
            }
            val sheetHeight by animateDpAsState(
                targetValue = if (setupExpanded) screenHeight * 0.90f else collapsedHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "first_launch_sheet_height"
            )
            val lowerSize = if (screenWidth < 520.dp) screenWidth * 0.76f else 420.dp
            val middleSize = if (screenWidth < 520.dp) screenWidth * 0.46f else 260.dp
            val upperLeftSize = if (screenWidth < 520.dp) screenWidth * 0.38f else 220.dp
            val upperRightSize = if (screenWidth < 520.dp) screenWidth * 0.52f else 300.dp
            val movingSize = if (screenWidth < 520.dp) 88.dp else 104.dp
            val motionProgress = remember { Animatable(0f) }
            val sheetReveal = remember { Animatable(if (reduceMotion) 1f else 0f) }

            LaunchedEffect(reduceMotion) {
                if (reduceMotion) {
                    sheetReveal.snapTo(1f)
                } else {
                    sheetReveal.snapTo(0f)
                    sheetReveal.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }

            LaunchedEffect(reduceMotion) {
                if (reduceMotion) {
                    motionProgress.snapTo(0f)
                } else {
                    while (true) {
                        motionProgress.snapTo(0f)
                        motionProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 4_200,
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

            DynamicBackdropMotionTracker {
                motionProgress.value to sheetHeight
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .dynamicBackdropSource(
                        DynamicBackdropLayer.BACKGROUND,
                        followMotion = true
                    )
            ) {
                Text(
                    text = stringResource(R.string.onboarding_brand),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 24.dp, top = 12.dp)
                )

                // Lowest obstacle: a large cookie emerging from the bottom-left corner.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = -(lowerSize * 0.22f),
                            y = screenHeight - lowerSize * 0.56f
                        )
                        .size(lowerSize)
                        .graphicsLayer {
                            rotationZ = -14f + motionFrame.lowerImpact * 5f
                            scaleX = 1f + motionFrame.lowerImpact * 0.08f
                            scaleY = 1f - motionFrame.lowerImpact * 0.10f
                            transformOrigin = TransformOrigin(0.45f, 0.80f)
                        }
                        .clip(MaterialShapes.Cookie12Sided.toShape())
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )

                // The right obstacle sits above the cookie and returns the shape towards the left.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = screenWidth - middleSize * 0.72f,
                            y = screenHeight * 0.58f
                        )
                        .size(middleSize)
                        .graphicsLayer {
                            rotationZ = 18f - motionFrame.middleImpact * 7f
                            scaleX = 1f - motionFrame.middleImpact * 0.09f
                            scaleY = 1f + motionFrame.middleImpact * 0.08f
                        }
                        .clip(MaterialShapes.Clover8Leaf.toShape())
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                // The third obstacle is higher on the left and redirects the Gem to the upper-right.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = -(upperLeftSize * 0.10f),
                            y = screenHeight * 0.34f
                        )
                        .size(upperLeftSize)
                        .graphicsLayer {
                            rotationZ = -20f + motionFrame.upperLeftImpact * 8f
                            scaleX = 1f + motionFrame.upperLeftImpact * 0.08f
                            scaleY = 1f - motionFrame.upperLeftImpact * 0.09f
                        }
                        .clip(MaterialShapes.Cookie6Sided.toShape())
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                )

                // A large final obstacle in the upper-right sends the Gem out through the left edge.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = screenWidth - upperRightSize * 0.72f,
                            y = screenHeight * 0.06f
                        )
                        .size(upperRightSize)
                        .graphicsLayer {
                            rotationZ = 14f - motionFrame.upperRightImpact * 7f
                            scaleX = 1f - motionFrame.upperRightImpact * 0.08f
                            scaleY = 1f + motionFrame.upperRightImpact * 0.09f
                            transformOrigin = TransformOrigin(0.35f, 0.65f)
                        }
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )

                // One medium Gem climbs through all four collisions in a continuous zigzag.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = screenWidth * motionFrame.movingXFraction,
                            y = screenHeight * motionFrame.movingYFraction
                        )
                        .size(movingSize)
                        .graphicsLayer {
                            rotationZ = motionFrame.movingRotation
                            scaleX = motionFrame.movingScaleX
                            scaleY = motionFrame.movingScaleY
                        }
                        .clip(MaterialShapes.Gem.toShape())
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            TranslucentBackdropSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .offset(y = sheetHeight * (1f - sheetReveal.value)),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                tint = MaterialTheme.colorScheme.surfaceContainer,
                blurRadius = 32.dp,
                backdropLayer = DynamicBackdropLayer.SHEET,
                followMotion = true,
                blurEnabled = true,
                tintAlpha = 0.72f
            ) {
                if (!setupExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { setupExpanded = true },
                            modifier = Modifier.weight(1.35f),
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                bottomStart = 28.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_start_setup),
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = onNotNow,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp,
                                topEnd = 28.dp,
                                bottomEnd = 28.dp
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_skip_setup),
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Spacer(Modifier.height(20.dp))
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
