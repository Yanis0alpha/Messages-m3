package com.anindra.messages.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object ConnectedButtonGroupLayout {
    val HEIGHT = 40.dp
    val MIN_WIDTH = 48.dp
    val GAP = 2.dp
    val OUTER_CORNER = 20.dp
    val INNER_CORNER = 8.dp
    val PRESSED_INNER_CORNER = 4.dp

    fun startCorner(
        index: Int,
        count: Int,
        selected: Boolean,
        pressed: Boolean = false
    ): Dp = when {
        selected || count == 1 || index == 0 -> OUTER_CORNER
        pressed -> PRESSED_INNER_CORNER
        else -> INNER_CORNER
    }

    fun endCorner(
        index: Int,
        count: Int,
        selected: Boolean,
        pressed: Boolean = false
    ): Dp = when {
        selected || count == 1 || index == count - 1 -> OUTER_CORNER
        pressed -> PRESSED_INNER_CORNER
        else -> INNER_CORNER
    }
}
