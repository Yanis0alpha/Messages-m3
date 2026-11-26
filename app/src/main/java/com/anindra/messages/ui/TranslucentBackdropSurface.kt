package com.anindra.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A clipped Material tint over a true Android 12+ backdrop blur. */
@Composable
internal fun TranslucentBackdropSurface(
    shape: Shape,
    tint: Color,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 32.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundRoot = LocalView.current.rootView
    val captureGuard = remember { BackdropCaptureGuard() }
    val surfaceAlpha = LocalTranslucentSurfaceAlpha.current
    var surfacePosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .clip(shape)
            .onGloballyPositioned { coordinates ->
                val current = coordinates.positionInWindow()
                if (surfacePosition != current) surfacePosition = current
            }
            .drawWithContent {
                if (!captureGuard.active) drawContent()
            }
    ) {
        DynamicBackdropBlur(
            backgroundRoot = backgroundRoot,
            samplePosition = surfacePosition,
            captureGuard = captureGuard,
            blurRadius = blurRadius,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(tint.copy(alpha = surfaceAlpha))
        )
        content()
    }
}
