package com.anindra.messages.ui

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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anindra.messages.R
import kotlinx.coroutines.launch

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

/** Pixel-inspired first-run setup with four static Material shapes and shared app glass. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FirstLaunchSetupScreen(
    defaultSmsReady: Boolean,
    permissionsReady: Boolean,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onComplete: () -> Unit,
    onNotNow: () -> Unit
) {
    val action = FirstLaunchSetupFlow.nextAction(defaultSmsReady, permissionsReady)
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    val surfaceAlpha = LocalTranslucentSurfaceAlpha.current
    var setupStarted by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            val proposedCollapsedHeight = screenHeight * 0.10f
            val collapsedHeight = if (proposedCollapsedHeight < 80.dp) {
                80.dp
            } else {
                proposedCollapsedHeight
            }
            val sheetHeight = screenHeight * ChatSheetLayout.HEIGHT_FRACTION
            val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

            DynamicBackdropMotionTracker {
                runCatching { scaffoldState.bottomSheetState.requireOffset() }.getOrNull()
            }

            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                modifier = Modifier.fillMaxSize(),
                sheetPeekHeight = collapsedHeight,
                sheetShape = sheetShape,
                sheetContainerColor = Color.Transparent,
                sheetTonalElevation = 0.dp,
                sheetShadowElevation = 8.dp,
                sheetDragHandle = null,
                sheetSwipeEnabled = setupStarted,
                containerColor = Color.Transparent,
                sheetContent = {
                    // Same 25 dp CONTENT blur as the cards on the main page. The source is now
                    // static, so only the sheet position changes while dragging.
                    TranslucentBackdropSurface(
                        shape = sheetShape,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(sheetHeight),
                        blurRadius = ChatSheetLayout.BACKGROUND_BLUR_RADIUS,
                        backdropLayer = DynamicBackdropLayer.CONTENT,
                        followMotion = true,
                        tintAlpha = surfaceAlpha
                    ) {
                        if (!setupStarted) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(collapsedHeight)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        setupStarted = true
                                        scope.launch {
                                            scaffoldState.bottomSheetState.expand()
                                        }
                                    },
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
                            Column(Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ChatSheetLayout.DRAG_HANDLE_HEIGHT),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BottomSheetDefaults.DragHandle()
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .navigationBarsPadding()
                                        .padding(horizontal = 24.dp, vertical = 14.dp),
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
                                            FirstLaunchAction.REQUEST_DEFAULT_SMS ->
                                                onRequestDefaultSms()
                                            FirstLaunchAction.REQUEST_PERMISSIONS ->
                                                onRequestPermissions()
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
            ) { _ ->
                Box(Modifier.fillMaxSize()) {
                    // Same stable three-layer backdrop used on the main page: source, one shared
                    // pre-blur, then a sharp visible copy. Nothing is re-rasterized by rotation.
                    FirstLaunchStaticBackdrop()
                    SettingsBlurredBackdropSource()
                    FirstLaunchStaticBackdrop(registerAsSource = false)

                    Text(
                        text = stringResource(R.string.onboarding_hero_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 24.dp, top = 18.dp)
                            .fillMaxWidth(0.58f)
                    )
                }
            }
        }
    }
}

/** Static M3 silhouettes arranged like the main page backdrop, without a moving Gem. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FirstLaunchStaticBackdrop(registerAsSource: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .then(
                if (registerAsSource) {
                    Modifier.dynamicBackdropSource(DynamicBackdropLayer.BACKGROUND)
                } else {
                    Modifier
                }
            )
            .background(colors.background)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 58.dp, y = (-48).dp)
                .size(216.dp)
                .rotate(14f)
                .background(
                    colors.primaryContainer.copy(alpha = 0.24f),
                    MaterialShapes.Cookie12Sided.toShape()
                )
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-70).dp, y = (-64).dp)
                .size(176.dp)
                .rotate(-21f)
                .background(
                    colors.tertiaryContainer.copy(alpha = 0.18f),
                    MaterialShapes.Clover8Leaf.toShape()
                )
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 52.dp, y = 96.dp)
                .size(156.dp)
                .rotate(10f)
                .background(
                    colors.secondaryContainer.copy(alpha = 0.18f),
                    MaterialShapes.Sunny.toShape()
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-52).dp, y = (-76).dp)
                .size(196.dp)
                .rotate(27f)
                .background(
                    colors.primaryContainer.copy(alpha = 0.16f),
                    MaterialShapes.Cookie9Sided.toShape()
                )
        )
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
