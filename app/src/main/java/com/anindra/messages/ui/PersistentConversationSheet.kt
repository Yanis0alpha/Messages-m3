package com.anindra.messages.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anindra.messages.R
import kotlinx.coroutines.launch

internal object PersistentConversationSheetLayout {
    const val PEEK_FRACTION = 0.1f
    const val EXPANDED_FRACTION = 0.9f
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PersistentConversationSheet(
    conversationOpen: Boolean,
    onCollapsed: () -> Unit,
    captureGuard: BackdropCaptureGuard,
    content: @Composable () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val sheetState = scaffoldState.bottomSheetState
    val scope = rememberCoroutineScope()
    val backgroundRoot = LocalView.current.rootView
    val surfaceAlpha = LocalTranslucentSurfaceAlpha.current
    var sheetPosition by remember { mutableStateOf(Offset.Zero) }
    var expandedConversation by remember { mutableStateOf(false) }

    LaunchedEffect(conversationOpen) {
        if (conversationOpen) {
            sheetState.expand()
            expandedConversation = true
        } else {
            expandedConversation = false
            if (sheetState.currentValue == SheetValue.Expanded) {
                sheetState.partialExpand()
            }
        }
    }
    LaunchedEffect(sheetState.currentValue, conversationOpen, expandedConversation) {
        if (conversationOpen && expandedConversation &&
            sheetState.currentValue == SheetValue.PartiallyExpanded &&
            !sheetState.isAnimationRunning
        ) {
            onCollapsed()
        }
        android.util.Log.d(
            "PersistentChatSheet",
            "state=${sheetState.currentValue} conversationOpen=$conversationOpen"
        )
    }

    BackHandler(
        enabled = !conversationOpen && sheetState.currentValue == SheetValue.Expanded
    ) {
        scope.launch { sheetState.partialExpand() }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sheetHeight = maxHeight * PersistentConversationSheetLayout.EXPANDED_FRACTION
        val peekHeight = maxHeight * PersistentConversationSheetLayout.PEEK_FRACTION
        val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        val toggleLabel = stringResource(
            if (sheetState.currentValue == SheetValue.Expanded) {
                R.string.common_close
            } else {
                R.string.access_open_conversation
            }
        )

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
                                MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha)
                            )
                    )
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ChatSheetLayout.DRAG_HANDLE_HEIGHT)
                                .clickable {
                                    scope.launch {
                                        if (sheetState.currentValue == SheetValue.Expanded) {
                                            sheetState.partialExpand()
                                        } else {
                                            sheetState.expand()
                                        }
                                    }
                                }
                                .semantics { contentDescription = toggleLabel },
                            contentAlignment = Alignment.Center
                        ) {
                            BottomSheetDefaults.DragHandle()
                        }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (conversationOpen) {
                                content()
                            } else {
                                PersistentConversationEmptyState()
                            }
                        }
                    }
                }
            }
        ) { }
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
