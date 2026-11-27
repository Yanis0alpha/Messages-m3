package com.anindra.messages.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.anindra.messages.data.TranslucentSurfaceOpacity

internal val LocalTranslucentSurfaceAlpha = staticCompositionLocalOf {
    TranslucentSurfaceOpacity.defaultAlpha
}

internal val LocalSettingsCardBlurEnabled = staticCompositionLocalOf { true }

internal val LocalSettingsCardAlpha = staticCompositionLocalOf {
    TranslucentSurfaceOpacity.defaultAlpha
}
