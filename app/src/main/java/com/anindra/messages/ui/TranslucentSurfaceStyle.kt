package com.anindra.messages.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.anindra.messages.data.TranslucentSurfaceOpacity

internal val LocalTranslucentSurfaceAlpha = staticCompositionLocalOf {
    TranslucentSurfaceOpacity.defaultAlpha
}

internal val LocalSettingsCardBlurEnabled = staticCompositionLocalOf { true }

internal val LocalSettingsCardAlpha = staticCompositionLocalOf {
    TranslucentSurfaceOpacity.defaultAlpha
}

/** Keep pastel M3 silhouettes visible over light surfaces without overpowering dark themes. */
internal fun expressiveShapeAlpha(
    background: Color,
    lightAlpha: Float,
    darkAlpha: Float
): Float = if (background.luminance() >= 0.5f) lightAlpha else darkAlpha
