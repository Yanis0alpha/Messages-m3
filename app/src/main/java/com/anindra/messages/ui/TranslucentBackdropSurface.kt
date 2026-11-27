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
    blurRadius: Dp = 25.dp,
    backdropLayer: Float = DynamicBackdropLayer.CONTENT,
    followMotion: Boolean = true,
    blurEnabled: Boolean = true,
    tintAlpha: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceAlpha = tintAlpha ?: LocalTranslucentSurfaceAlpha.current

    Box(
        modifier
            .clip(shape)
            // Cards sample the shared layer below them. Their own pixels are not registered as
            // independent sources, which keeps long Settings lists to one recorded backdrop.
            .then(
                if (blurEnabled) {
                    Modifier.dynamicBackdropEffect(blurRadius, backdropLayer, followMotion)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(tint.copy(alpha = surfaceAlpha))
        )
        content()
    }
}
