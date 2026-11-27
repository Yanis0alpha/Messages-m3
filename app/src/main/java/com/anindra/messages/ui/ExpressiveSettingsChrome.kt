package com.anindra.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anindra.messages.R

/**
 * Shared expressive backdrop for modern Settings sub-pages. The first copy is
 * registered for glass, the middle layer caches one blur for every Settings
 * card, and the last copy remains sharp on screen.
 */
@Composable
internal fun ExpressiveSettingsBackdrop() {
    SettingsExpressiveBackdrop()
    SettingsBlurredBackdropSource()
    SettingsExpressiveBackdrop(registerAsSource = false)
}

/** Records the moving sub-page body so its glass app bar samples the current frame. */
internal fun Modifier.expressiveSettingsContentSource(): Modifier =
    dynamicBackdropSource(DynamicBackdropLayer.CONTENT, followMotion = true)

/** One chrome implementation for every modern Settings destination. */
@Composable
internal fun ExpressiveSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleContent: @Composable () -> Unit = { Text(title) },
    navigationContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    Box(modifier.fillMaxSize()) {
        ExpressiveSettingsBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            topBar = {
                ExpressiveSettingsTopBar(
                    title = title,
                    onBack = onBack,
                    titleContent = titleContent,
                    navigationContentDescription = navigationContentDescription,
                    actions = actions
                )
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .expressiveSettingsContentSource()
            ) {
                content(padding)
            }
        }
    }
}

/** Android-style Settings app bar, attached to the top with expressive rounded lower corners. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpressiveSettingsTopBar(
    title: String,
    onBack: () -> Unit,
    titleContent: @Composable () -> Unit = { Text(title) },
    navigationContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val alpha = LocalTranslucentSurfaceAlpha.current
    val shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    TopAppBar(
        modifier = Modifier
            .clip(shape)
            .dynamicBackdropSource(DynamicBackdropLayer.HEADER)
            .dynamicBackdropEffect(
                25.dp,
                DynamicBackdropLayer.HEADER,
                followMotion = true
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        title = titleContent,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = navigationContentDescription
                        ?: stringResource(R.string.icon_back)
                )
            }
        },
        actions = actions
    )
}

/** A colored, optionally blurred card shared by modern Settings sub-pages. */
@Composable
internal fun ExpressiveSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    TranslucentBackdropSurface(
        shape = RoundedCornerShape(24.dp),
        tint = settingsCardTint(),
        modifier = modifier,
        blurRadius = 0.dp,
        backdropLayer = DynamicBackdropLayer.SETTINGS_CARD,
        followMotion = true,
        blurEnabled = LocalSettingsCardBlurEnabled.current,
        tintAlpha = LocalSettingsCardAlpha.current,
        content = content
    )
}

/** Shared modern modal-sheet style: same-frame blur, tint and drag handle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpressiveSettingsBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit
) {
    DynamicBackdropMotionTracker {
        runCatching { sheetState.requireOffset() }.getOrNull()
    }
    val alpha = LocalTranslucentSurfaceAlpha.current
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = ChatSheetLayout.SCRIM_ALPHA),
        dragHandle = null
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .dynamicBackdropEffect(
                    25.dp,
                    DynamicBackdropLayer.SHEET,
                    followMotion = true
                )
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha))
            )
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ChatSheetLayout.DRAG_HANDLE_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle()
                }
                content()
            }
        }
    }
}
