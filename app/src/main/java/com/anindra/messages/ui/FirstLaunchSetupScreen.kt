package com.anindra.messages.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anindra.messages.R
import com.anindra.messages.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

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
            val bounceY = visualHeight.value * 0.34f
            val gemY = remember { Animatable(-112f) }

            LaunchedEffect(reduceMotion, bounceY) {
                if (reduceMotion) {
                    gemY.snapTo(bounceY - 22f)
                } else {
                    while (true) {
                        gemY.snapTo(-112f)
                        gemY.animateTo(
                            targetValue = bounceY,
                            animationSpec = tween(
                                durationMillis = 760,
                                easing = FastOutSlowInEasing
                            )
                        )
                        gemY.animateTo(
                            targetValue = bounceY - 34f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        gemY.animateTo(
                            targetValue = bounceY,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        delay(220)
                        gemY.animateTo(
                            targetValue = -72f,
                            animationSpec = tween(
                                durationMillis = 780,
                                easing = FastOutSlowInEasing
                            )
                        )
                        delay(520)
                    }
                }
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
                            .offset(x = -(cookieSize * 0.28f), y = cookieSize * 0.30f)
                            .size(cookieSize)
                            .rotate(-12f)
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-48).dp, y = gemY.value.dp)
                            .size(96.dp)
                            .graphicsLayer {
                                rotationZ = (gemY.value + 112f) * 0.34f
                            }
                            .clip(MaterialShapes.Gem.toShape())
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
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
private fun SetupRequirementCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    complete: Boolean,
    onClick: () -> Unit
) {
    val container = if (complete) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(
        modifier = Modifier
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
