package com.anindra.messages.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anindra.messages.ui.theme.LocalReduceMotion
import com.anindra.messages.ui.theme.motionSpring

internal data class AttachAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

internal object AttachmentCarouselMetrics {
    const val ITEM_WIDTH_DP = 104f
    const val ITEM_HEIGHT_DP = 96f
    const val ITEM_SPACING_DP = 8f
    const val HORIZONTAL_PADDING_DP = 8f
    const val OUTER_HORIZONTAL_PADDING_DP = 12f
    const val OUTER_VERTICAL_PADDING_DP = 4f
    const val ITEM_CORNER_DP = 24f
}

@Composable
internal fun AttachmentCarousel(
    onCamera: () -> Unit,
    cameraLabel: String,
    cameraIcon: ImageVector,
    actions: List<AttachAction>,
    modifier: Modifier = Modifier
) {
    val items = listOf(AttachAction(cameraLabel, cameraIcon, onCamera)) + actions
    val colors = MaterialTheme.colorScheme
    val accents = listOf(colors.primary, colors.tertiary, colors.secondary)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AttachmentCarouselMetrics.OUTER_HORIZONTAL_PADDING_DP.dp,
                vertical = AttachmentCarouselMetrics.OUTER_VERTICAL_PADDING_DP.dp
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        HorizontalUncontainedCarousel(
            state = rememberCarouselState { items.size },
            itemWidth = AttachmentCarouselMetrics.ITEM_WIDTH_DP.dp,
            itemSpacing = AttachmentCarouselMetrics.ITEM_SPACING_DP.dp,
            contentPadding = PaddingValues(
                horizontal = AttachmentCarouselMetrics.HORIZONTAL_PADDING_DP.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    AttachmentCarouselMetrics.ITEM_HEIGHT_DP.dp +
                        AttachmentCarouselMetrics.ITEM_SPACING_DP.dp * 2
                )
                .padding(vertical = AttachmentCarouselMetrics.ITEM_SPACING_DP.dp)
        ) { index ->
            val item = items[index]
            AttachmentCarouselItem(
                action = item,
                iconTint = accents[index % accents.size],
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AttachmentCarouselMetrics.ITEM_HEIGHT_DP.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge)
            )
        }
    }
}

@Composable
internal fun AttachmentCarouselItem(
    action: AttachAction,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = motionSpring(
            LocalReduceMotion.current,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "attachmentCarouselPress"
    )

    Surface(
        onClick = action.onClick,
        interactionSource = interaction,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
