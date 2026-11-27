package com.anindra.messages.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Shared layer ordering for same-frame backdrop composition. */
internal object DynamicBackdropLayer {
    const val BACKGROUND = 0f
    const val CONTENT = 1f
    const val FLOATING_CONTENT = 2f
    const val HEADER = 3f
    const val NAVIGATION = 4f
    const val SHEET = 5f
}

private val LocalDynamicBackdropState = staticCompositionLocalOf<HazeState?> { null }

/** One hardware-accelerated backdrop graph shared by every glass surface. */
@Composable
internal fun DynamicBackdropHost(content: @Composable () -> Unit) {
    val state = rememberHazeState()
    CompositionLocalProvider(LocalDynamicBackdropState provides state, content = content)
}

/** Makes this content available to glass surfaces above it without bitmap capture. */
internal fun Modifier.dynamicBackdropSource(
    zIndex: Float,
    key: Any? = null
): Modifier = composed {
    val state = LocalDynamicBackdropState.current
    if (state == null) this else hazeSource(state = state, zIndex = zIndex, key = key)
}

/** Draws a live blur of lower shared layers in the same Compose frame. */
@OptIn(ExperimentalHazeApi::class)
internal fun Modifier.dynamicBackdropEffect(
    blurRadius: Dp = 32.dp
): Modifier = composed {
    val state = LocalDynamicBackdropState.current
    if (state == null) {
        this
    } else {
        val fallback = MaterialTheme.colorScheme.surface
        hazeEffect(
            state = state,
            style = HazeStyle(
                backgroundColor = fallback,
                tints = emptyList(),
                blurRadius = blurRadius,
                noiseFactor = 0f
            )
        ) {
            // Reducing the sampled pixel count avoids the scrolling stalls of
            // the former per-surface root bitmap captures without a visible loss.
            inputScale = HazeInputScale.Fixed(0.66f)
        }
    }
}

/** A live backdrop layer; callers draw their translucent Material tint above it. */
@Composable
internal fun BoxScope.DynamicBackdropBlur(
    modifier: Modifier,
    blurRadius: Dp = 32.dp
) {
    Box(modifier.dynamicBackdropEffect(blurRadius))
}
