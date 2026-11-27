package com.anindra.messages.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anindra.messages.R
import com.anindra.messages.ui.theme.LocalReduceMotion
import com.anindra.messages.ui.theme.Motion
import com.anindra.messages.ui.theme.motionSpring
import com.anindra.messages.ui.theme.motionTween
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal object PersistentConversationSheetLayout {
    const val PEEK_FRACTION = 0.1f
    const val EXPANDED_FRACTION = 0.9f
    const val ACTION_TIMEOUT_SECONDS = 3
    const val ACTION_TICK_MS = 1_000L
    const val DISMISS_PREVIEW_THRESHOLD = 0.5f

    fun peekHeight(containerHeight: Dp): Dp = containerHeight * PEEK_FRACTION
}

internal fun persistentSheetCollapseProgress(
    sheetTop: Float,
    expandedTop: Float,
    collapsedTop: Float
): Float {
    val range = (collapsedTop - expandedTop).coerceAtLeast(1f)
    return ((sheetTop - expandedTop) / range).coerceIn(0f, 1f)
}

internal fun shouldShowPersistentConversationSheet(
    useNewUi: Boolean,
    chatUsesSheet: Boolean,
    navRoute: String,
    topLevelDestination: TopLevelDestination
): Boolean = useNewUi && chatUsesSheet && (
    navRoute == "chat" ||
        (navRoute == "list" && topLevelDestination == TopLevelDestination.MESSAGES)
)

private enum class PersistentConversationSheetMode {
    ACTIVE,
    MINIMIZE_REQUESTED,
    MINIMIZED
}

private enum class PersistentConversationHandleMode {
    HANDLE,
    ACTIONS,
    MINIMIZED
}

@OptIn(ExperimentalMaterial3Api::class)
private data class PersistentSheetSnapshot(
    val currentValue: SheetValue,
    val targetValue: SheetValue,
    val animationRunning: Boolean,
    val mode: PersistentConversationSheetMode
)

@OptIn(ExperimentalMaterial3Api::class)
internal fun shouldClosePersistentConversation(
    conversationOpen: Boolean,
    collapseArmed: Boolean,
    keepConversationOpen: Boolean,
    currentValue: SheetValue,
    targetValue: SheetValue,
    animationRunning: Boolean
): Boolean = conversationOpen && collapseArmed && !keepConversationOpen &&
    currentValue == SheetValue.PartiallyExpanded &&
    targetValue == SheetValue.PartiallyExpanded &&
    !animationRunning

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PersistentConversationSheet(
    conversationOpen: Boolean,
    conversationTitle: String,
    conversationAddress: String,
    onCollapsed: () -> Unit,
    captureGuard: BackdropCaptureGuard,
    peekHeight: Dp,
    content: @Composable () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val sheetState = scaffoldState.bottomSheetState
    val scope = rememberCoroutineScope()
    val backgroundRoot = LocalView.current.rootView
    val surfaceAlpha = LocalTranslucentSurfaceAlpha.current
    var sheetPosition by remember { mutableStateOf(Offset.Zero) }
    var expandedConversation by remember { mutableStateOf(false) }
    var sheetMode by remember { mutableStateOf(PersistentConversationSheetMode.ACTIVE) }
    var handleActionsVisible by remember { mutableStateOf(false) }
    var showMinimizedSummary by remember { mutableStateOf(false) }
    var actionCountdownSeconds by remember { mutableIntStateOf(0) }
    var sheetTopInRoot by remember { mutableFloatStateOf(0f) }
    val latestOnCollapsed by rememberUpdatedState(onCollapsed)

    LaunchedEffect(conversationOpen) {
        if (conversationOpen) {
            sheetMode = PersistentConversationSheetMode.ACTIVE
            showMinimizedSummary = false
            sheetState.expand()
            expandedConversation = true
        } else {
            expandedConversation = false
            sheetMode = PersistentConversationSheetMode.ACTIVE
            handleActionsVisible = false
            showMinimizedSummary = false
            if (sheetState.currentValue == SheetValue.Expanded) {
                sheetState.partialExpand()
            }
        }
    }
    LaunchedEffect(handleActionsVisible, conversationOpen) {
        if (!handleActionsVisible || !conversationOpen) {
            actionCountdownSeconds = 0
            return@LaunchedEffect
        }
        actionCountdownSeconds = PersistentConversationSheetLayout.ACTION_TIMEOUT_SECONDS
        for (remaining in
            (PersistentConversationSheetLayout.ACTION_TIMEOUT_SECONDS - 1) downTo 0
        ) {
            delay(PersistentConversationSheetLayout.ACTION_TICK_MS)
            if (remaining == 0) {
                handleActionsVisible = false
            } else {
                actionCountdownSeconds = remaining
            }
        }
    }
    LaunchedEffect(sheetState, conversationOpen, expandedConversation) {
        snapshotFlow {
            PersistentSheetSnapshot(
                currentValue = sheetState.currentValue,
                targetValue = sheetState.targetValue,
                animationRunning = sheetState.isAnimationRunning,
                mode = sheetMode
            )
        }.collect { snapshot ->
            val settledCollapsed =
                snapshot.currentValue == SheetValue.PartiallyExpanded &&
                    snapshot.targetValue == SheetValue.PartiallyExpanded &&
                    !snapshot.animationRunning
            val settledExpanded =
                snapshot.currentValue == SheetValue.Expanded &&
                    snapshot.targetValue == SheetValue.Expanded &&
                    !snapshot.animationRunning

            if (snapshot.mode == PersistentConversationSheetMode.MINIMIZED) {
                showMinimizedSummary =
                    snapshot.targetValue != SheetValue.Expanded
            }

            when {
                settledCollapsed &&
                    snapshot.mode == PersistentConversationSheetMode.MINIMIZE_REQUESTED -> {
                    // Keep the route and ChatScreen composition alive behind
                    // the 10% peek until the user pulls the sheet up again.
                    sheetMode = PersistentConversationSheetMode.MINIMIZED
                    showMinimizedSummary = true
                }
                settledExpanded &&
                    snapshot.mode == PersistentConversationSheetMode.MINIMIZED -> {
                    sheetMode = PersistentConversationSheetMode.ACTIVE
                    handleActionsVisible = false
                    showMinimizedSummary = false
                }
                shouldClosePersistentConversation(
                    conversationOpen = conversationOpen,
                    collapseArmed = expandedConversation,
                    keepConversationOpen =
                        snapshot.mode != PersistentConversationSheetMode.ACTIVE,
                    currentValue = snapshot.currentValue,
                    targetValue = snapshot.targetValue,
                    animationRunning = snapshot.animationRunning
                ) -> {
                    // A physical downward dismissal still behaves exactly like
                    // Android Back and can close/persist the conversation once.
                    expandedConversation = false
                    handleActionsVisible = false
                    showMinimizedSummary = false
                    latestOnCollapsed()
                }
            }
            android.util.Log.d(
                "PersistentChatSheet",
                "state=${snapshot.currentValue} target=${snapshot.targetValue} " +
                    "running=${snapshot.animationRunning} mode=${snapshot.mode} " +
                    "conversationOpen=$conversationOpen"
            )
        }
    }

    BackHandler(
        enabled = !conversationOpen && sheetState.currentValue == SheetValue.Expanded
    ) {
        scope.launch { sheetState.partialExpand() }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sheetHeight = maxHeight * PersistentConversationSheetLayout.EXPANDED_FRACTION
        val density = LocalDensity.current
        val expandedTopPx = with(density) { (maxHeight - sheetHeight).toPx() }
        val collapsedTopPx = with(density) { (maxHeight - peekHeight).toPx() }
        val collapseProgress = persistentSheetCollapseProgress(
            sheetTop = sheetTopInRoot,
            expandedTop = expandedTopPx,
            collapsedTop = collapsedTopPx
        )
        val dragDismissPreviewVisible = conversationOpen &&
            expandedConversation &&
            sheetMode == PersistentConversationSheetMode.ACTIVE &&
            collapseProgress >= PersistentConversationSheetLayout.DISMISS_PREVIEW_THRESHOLD
        val conversationContentProgress by animateFloatAsState(
            targetValue = if (showMinimizedSummary || dragDismissPreviewVisible) 0f else 1f,
            animationSpec = motionTween(
                LocalReduceMotion.current,
                Motion.DURATION_SHORT4
            ),
            label = "persistentConversationContent"
        )
        val emptyStateProgress by animateFloatAsState(
            targetValue = if (dragDismissPreviewVisible) 1f else 0f,
            animationSpec = motionTween(
                LocalReduceMotion.current,
                Motion.DURATION_SHORT4
            ),
            label = "persistentConversationEmptyState"
        )
        val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        val openConversationLabel = stringResource(R.string.access_open_conversation)
        val handleLabel = if (showMinimizedSummary) {
            openConversationLabel
        } else if (conversationOpen) {
            stringResource(R.string.conversation_sheet_actions)
        } else {
            stringResource(
                if (sheetState.currentValue == SheetValue.Expanded) {
                    R.string.common_close
                } else {
                    R.string.access_open_conversation
                }
            )
        }
        val restoreMinimizedConversation: () -> Unit = {
            handleActionsVisible = false
            showMinimizedSummary = false
            scope.launch {
                // Keep MINIMIZE_REQUESTED/MINIMIZED armed while expanding so
                // the old partial anchor cannot be mistaken for a close.
                sheetState.expand()
                sheetMode = PersistentConversationSheetMode.ACTIVE
            }
        }

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            modifier = Modifier.fillMaxSize(),
            sheetPeekHeight = peekHeight,
            sheetShape = sheetShape,
            sheetContainerColor = Color.Transparent,
            sheetTonalElevation = 0.dp,
            sheetShadowElevation = 8.dp,
            sheetDragHandle = null,
            containerColor = Color.Transparent,
            sheetContent = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .clip(sheetShape)
                        .onGloballyPositioned { coordinates ->
                            val current = coordinates.positionInWindow()
                            if (sheetPosition != current) sheetPosition = current
                            val currentTopInRoot = coordinates.positionInRoot().y
                            if (sheetTopInRoot != currentTopInRoot) {
                                sheetTopInRoot = currentTopInRoot
                            }
                        }
                        .drawWithContent {
                            if (!captureGuard.active) drawContent()
                        }
                ) {
                    DynamicBackdropBlur(
                        backgroundRoot = backgroundRoot,
                        samplePosition = sheetPosition,
                        captureGuard = captureGuard,
                        blurRadius = ChatSheetLayout.BACKGROUND_BLUR_RADIUS,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                    alpha = surfaceAlpha
                                )
                            )
                    )
                    Column(Modifier.fillMaxSize()) {
                        PersistentConversationSheetHandle(
                            conversationOpen = conversationOpen,
                            actionsVisible = handleActionsVisible,
                            minimizedSummaryVisible = showMinimizedSummary,
                            countdownSeconds = actionCountdownSeconds,
                            conversationTitle = conversationTitle,
                            conversationAddress = conversationAddress,
                            handleLabel = handleLabel,
                            onHandleClick = {
                                if (conversationOpen &&
                                    sheetMode != PersistentConversationSheetMode.ACTIVE
                                ) {
                                    restoreMinimizedConversation()
                                } else if (conversationOpen) {
                                    actionCountdownSeconds =
                                        PersistentConversationSheetLayout.ACTION_TIMEOUT_SECONDS
                                    handleActionsVisible = true
                                } else {
                                    scope.launch {
                                        if (sheetState.currentValue == SheetValue.Expanded) {
                                            sheetState.partialExpand()
                                        } else {
                                            sheetState.expand()
                                        }
                                    }
                                }
                            },
                            onClose = {
                                handleActionsVisible = false
                                showMinimizedSummary = false
                                sheetMode = PersistentConversationSheetMode.ACTIVE
                                expandedConversation = false
                                latestOnCollapsed()
                            },
                            onMinimize = {
                                handleActionsVisible = false
                                showMinimizedSummary = true
                                sheetMode =
                                    PersistentConversationSheetMode.MINIMIZE_REQUESTED
                                scope.launch { sheetState.partialExpand() }
                            }
                        )
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (conversationOpen) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = conversationContentProgress
                                            val scale = 0.98f +
                                                (0.02f * conversationContentProgress)
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .then(
                                            if (showMinimizedSummary ||
                                                dragDismissPreviewVisible
                                            ) {
                                                Modifier.clearAndSetSemantics { }
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    // Keep ChatScreen composed so a minimized
                                    // conversation retains its draft and scroll.
                                    content()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { alpha = emptyStateProgress }
                                        .then(
                                            if (dragDismissPreviewVisible) {
                                                Modifier
                                            } else {
                                                Modifier.clearAndSetSemantics { }
                                            }
                                        ),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Box(Modifier.padding(top = 24.dp)) {
                                        PersistentConversationEmptyState()
                                    }
                                }
                            } else {
                                PersistentConversationEmptyState()
                            }
                        }
                    }
                    if (showMinimizedSummary) {
                        // During the collapse and in the final 10% peek, the
                        // complete visible sheet restores this conversation.
                        // It also blocks taps on the fading ChatScreen below.
                        Box(
                            Modifier
                                .matchParentSize()
                                .clickable(onClick = restoreMinimizedConversation)
                                .semantics {
                                    contentDescription = openConversationLabel
                                }
                        )
                    }
                }
            }
        ) { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersistentConversationSheetHandle(
    conversationOpen: Boolean,
    actionsVisible: Boolean,
    minimizedSummaryVisible: Boolean,
    countdownSeconds: Int,
    conversationTitle: String,
    conversationAddress: String,
    handleLabel: String,
    onHandleClick: () -> Unit,
    onClose: () -> Unit,
    onMinimize: () -> Unit
) {
    val handleMode = when {
        conversationOpen && minimizedSummaryVisible ->
            PersistentConversationHandleMode.MINIMIZED
        conversationOpen && actionsVisible ->
            PersistentConversationHandleMode.ACTIONS
        else -> PersistentConversationHandleMode.HANDLE
    }
    val reduceMotion = LocalReduceMotion.current
    val enterSpec = motionSpring<Float>(reduceMotion)
    val exitSpec = motionTween<Float>(reduceMotion, Motion.DURATION_SHORT4)
    val handleInteraction = if (handleMode == PersistentConversationHandleMode.ACTIONS) {
        Modifier
    } else {
        Modifier
            .clickable(onClick = onHandleClick)
            .semantics { contentDescription = handleLabel }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChatSheetLayout.DRAG_HANDLE_HEIGHT)
            .then(handleInteraction),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = handleMode,
            transitionSpec = {
                (fadeIn(enterSpec) +
                    scaleIn(initialScale = 0.35f, animationSpec = enterSpec))
                    .togetherWith(
                        fadeOut(exitSpec) +
                            scaleOut(targetScale = 0.35f, animationSpec = exitSpec)
                    )
            },
            contentAlignment = Alignment.Center,
            label = "persistentConversationHandleActions"
        ) { mode ->
            when (mode) {
                PersistentConversationHandleMode.ACTIONS -> {
                    val visibleCountdown = countdownSeconds.coerceAtLeast(1)
                    val countdownLabel = stringResource(
                        R.string.conversation_sheet_actions_timeout,
                        visibleCountdown
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                ConnectedButtonGroupLayout.GAP
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PersistentSheetActionButton(
                                index = 0,
                                label = stringResource(R.string.common_close),
                                primaryAction = true,
                                onClick = onClose
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            PersistentSheetActionButton(
                                index = 1,
                                label = stringResource(R.string.conversation_sheet_minimize),
                                primaryAction = false,
                                onClick = onMinimize
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .size(ConnectedButtonGroupLayout.HEIGHT)
                                .clearAndSetSemantics {
                                    contentDescription = countdownLabel
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = visibleCountdown.toString(),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
                PersistentConversationHandleMode.MINIMIZED -> {
                    MinimizedConversationPill(
                        title = conversationTitle,
                        address = conversationAddress
                    )
                }
                PersistentConversationHandleMode.HANDLE -> {
                    BottomSheetDefaults.DragHandle()
                }
            }
        }
    }
}

@Composable
private fun MinimizedConversationPill(title: String, address: String) {
    val label = buildList {
        if (title.isNotBlank()) add(title)
        if (address.isNotBlank() && address != title) add(address)
    }.joinToString("  ·  ")

    Surface(
        modifier = Modifier
            .height(ConnectedButtonGroupLayout.HEIGHT)
            .widthIn(max = 280.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PersistentSheetActionButton(
    index: Int,
    label: String,
    primaryAction: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val startCorner by animateDpAsState(
        targetValue = ConnectedButtonGroupLayout.startCorner(
            index = index,
            count = 2,
            selected = false,
            pressed = pressed
        ),
        animationSpec = motionSpring(reduceMotion),
        label = "persistentSheetActionStartCorner"
    )
    val endCorner by animateDpAsState(
        targetValue = ConnectedButtonGroupLayout.endCorner(
            index = index,
            count = 2,
            selected = false,
            pressed = pressed
        ),
        animationSpec = motionSpring(reduceMotion),
        label = "persistentSheetActionEndCorner"
    )
    val colors = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = Modifier.height(ConnectedButtonGroupLayout.HEIGHT),
        shape = RoundedCornerShape(
            topStart = startCorner,
            topEnd = endCorner,
            bottomEnd = endCorner,
            bottomStart = startCorner
        ),
        color = if (primaryAction) colors.primaryContainer else colors.secondaryContainer,
        contentColor = if (primaryAction) {
            colors.onPrimaryContainer
        } else {
            colors.onSecondaryContainer
        },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PersistentConversationEmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.conversation_sheet_nothing_open),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
