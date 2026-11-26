package com.anindra.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anindra.messages.R

internal object TopLevelHeaderDockLayout {
    const val TINT_ALPHA = 0.30f
    val HEIGHT = 168.dp
    val BOTTOM_CORNER = 32.dp
    val TOP_CONTROL_HEIGHT = 64.dp
    val SEARCH_HEIGHT = 56.dp
}

/**
 * The edge-attached glass dock shared by Messages, Archives and Settings.
 * Its background reaches the top and both side edges, while only the two
 * bottom corners round away like the exposed edge of a sheet.
 */
@Composable
internal fun TopLevelHeaderDock(
    title: String,
    modifier: Modifier = Modifier,
    topContent: @Composable BoxScope.() -> Unit
) {
    val backgroundRoot = LocalView.current.rootView
    val captureGuard = remember { BackdropCaptureGuard() }
    var dockPosition by remember { mutableStateOf(Offset.Zero) }
    val dockShape = RoundedCornerShape(
        bottomStart = TopLevelHeaderDockLayout.BOTTOM_CORNER,
        bottomEnd = TopLevelHeaderDockLayout.BOTTOM_CORNER
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(TopLevelHeaderDockLayout.HEIGHT),
        shape = dockShape,
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(dockShape)
                .onGloballyPositioned { coordinates ->
                    val current = coordinates.positionInWindow()
                    if (dockPosition != current) dockPosition = current
                }
                .drawWithContent {
                    if (!captureGuard.active) drawContent()
                }
        ) {
            DynamicBackdropBlur(
                backgroundRoot = backgroundRoot,
                samplePosition = dockPosition,
                captureGuard = captureGuard,
                blurRadius = 32.dp,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = TopLevelHeaderDockLayout.TINT_ALPHA
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TopLevelHeaderDockLayout.TOP_CONTROL_HEIGHT),
                    contentAlignment = Alignment.Center,
                    content = topContent
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** The always-visible search control placed at the top of [TopLevelHeaderDock]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TopLevelDockSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
    onClear: () -> Unit = { onQueryChange("") }
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, maxLines = 1) },
        singleLine = true,
        leadingIcon = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Search,
                        stringResource(R.string.icon_search),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.icon_close_search))
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(TopLevelHeaderDockLayout.SEARCH_HEIGHT / 2f),
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                alpha = 0.78f
            ),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                alpha = 0.68f
            ),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(TopLevelHeaderDockLayout.SEARCH_HEIGHT)
            .onFocusChanged { onFocusChanged(it.isFocused) }
    )
}
