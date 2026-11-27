package com.anindra.messages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A clipped Material tint over the shared same-frame backdrop blur. */
@Composable
internal fun TranslucentBackdropSurface(
    shape: Shape,
    tint: Color,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 32.dp,
    backdropLayer: Float = DynamicBackdropLayer.CONTENT,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceAlpha = LocalTranslucentSurfaceAlpha.current

    Box(
        modifier
            .clip(shape)
            // Modifier order matters: higher glass can sample this surface,
            // while its own effect only draws sources with a lower layer.
            .dynamicBackdropSource(backdropLayer)
            .dynamicBackdropEffect(blurRadius)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(tint.copy(alpha = surfaceAlpha))
        )
        content()
    }
}
