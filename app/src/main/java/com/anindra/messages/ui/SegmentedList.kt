package com.anindra.messages.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object SegmentedListLayout {
    val HORIZONTAL_PADDING: Dp = 12.dp
    val VERTICAL_PADDING: Dp = 4.dp
    val ITEM_GAP: Dp = 2.dp
}

internal fun segmentedListPosition(index: Int, size: Int): RowPosition = when {
    size <= 1 -> RowPosition.SINGLE
    index <= 0 -> RowPosition.FIRST
    index >= size - 1 -> RowPosition.LAST
    else -> RowPosition.MIDDLE
}

internal fun segmentedListShape(position: RowPosition): RoundedCornerShape {
    val corners = rowCorners(position)
    return RoundedCornerShape(
        topStart = corners.topStart,
        topEnd = corners.topEnd,
        bottomEnd = corners.bottomEnd,
        bottomStart = corners.bottomStart
    )
}
