package com.anindra.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anindra.messages.R

enum class TopLevelDestination {
    MESSAGES,
    ARCHIVED,
    SETTINGS
}

internal object TopLevelDestinationTransition {
    // Material fade-through: the old destination leaves first, then the new
    // destination appears. Keeping the phases separate avoids a muddy overlap.
    const val OUTGOING_DURATION_MS = 90
    const val INCOMING_DURATION_MS = 180
}

internal fun isTopLevelRouteTransition(from: String, to: String): Boolean {
    val contentRoutes = setOf("list", "chat")
    return (from == "settings" && to in contentRoutes) ||
        (to == "settings" && from in contentRoutes)
}

internal val TopLevelNavigationBarClearance = 104.dp

private data class TopLevelNavigationItem(
    val destination: TopLevelDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
internal fun MessagesNavigationBar(
    selectedDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    sharedCaptureGuard: BackdropCaptureGuard? = null,
    applyNavigationBarsPadding: Boolean = true
) {
    val backgroundRoot = LocalView.current.rootView
    val surfaceAlpha = LocalTranslucentSurfaceAlpha.current
    val localCaptureGuard = remember { BackdropCaptureGuard() }
    val captureGuard = sharedCaptureGuard ?: localCaptureGuard
    var barPosition by remember { mutableStateOf(Offset.Zero) }
    val barShape = RoundedCornerShape(32.dp)
    val items = listOf(
        TopLevelNavigationItem(
            destination = TopLevelDestination.MESSAGES,
            label = stringResource(R.string.conversations_messages),
            selectedIcon = Icons.AutoMirrored.Filled.Chat,
            unselectedIcon = Icons.AutoMirrored.Outlined.Chat
        ),
        TopLevelNavigationItem(
            destination = TopLevelDestination.ARCHIVED,
            label = stringResource(R.string.conversations_archived),
            selectedIcon = Icons.Rounded.Archive,
            unselectedIcon = Icons.Outlined.Archive
        ),
        TopLevelNavigationItem(
            destination = TopLevelDestination.SETTINGS,
            label = stringResource(R.string.navigation_settings),
            selectedIcon = Icons.Rounded.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    val systemBarPadding = if (applyNavigationBarsPadding) {
        Modifier.navigationBarsPadding()
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(systemBarPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            shape = barShape,
            color = Color.Transparent,
            shadowElevation = 8.dp
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(barShape)
                    .onGloballyPositioned { coordinates ->
                        val current = coordinates.positionInWindow()
                        if (barPosition != current) barPosition = current
                    }
                    // A same-window capture would otherwise draw this bar into
                    // its own backdrop. Skip it only during that nested draw.
                    .drawWithContent {
                        if (!captureGuard.active) drawContent()
                    }
            ) {
                DynamicBackdropBlur(
                    backgroundRoot = backgroundRoot,
                    samplePosition = barPosition,
                    captureGuard = captureGuard,
                    blurRadius = 32.dp,
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
                NavigationBar(
                    modifier = Modifier.matchParentSize(),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    items.forEach { item ->
                        val selected = selectedDestination == item.destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onDestinationSelected(item.destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(item.label, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
