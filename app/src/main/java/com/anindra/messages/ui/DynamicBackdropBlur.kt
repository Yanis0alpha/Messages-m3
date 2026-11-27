package com.anindra.messages.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlurEffect

/** Shared layer ordering for same-frame backdrop composition. */
internal object DynamicBackdropLayer {
    const val BACKGROUND = 0f
    const val CONTENT = 1f
    const val FLOATING_CONTENT = 2f
    const val HEADER = 3f
    const val NAVIGATION = 4f
    const val SHEET = 5f
    // Sheet children are above the sheet backdrop and must never be fed back into it.
    const val SHEET_CONTENT = 6f
    const val SHEET_FLOATING_CONTENT = 7f
}

@Stable
private class DynamicBackdropGraph(
    private val layers: List<Pair<Float, LayerBackdrop>>
) {
    fun source(layer: Float): LayerBackdrop? = layers.firstOrNull { it.first == layer }?.second

    fun below(layer: Float): Backdrop? {
        // Chat menu pills live inside a sheet. Sampling only that sheet's content avoids mixing
        // coordinates from a simultaneously composed two-pane inbox or from the sheet itself.
        val lower = layers.asSequence()
            .filter { (sourceLayer, _) ->
                if (layer == DynamicBackdropLayer.SHEET_FLOATING_CONTENT) {
                    sourceLayer == DynamicBackdropLayer.SHEET_CONTENT
                } else {
                    sourceLayer < layer
                }
            }
            .map { it.second }
            .toList()
        return when (lower.size) {
            0 -> null
            1 -> lower.first()
            else -> CombinedBackdrop(lower)
        }
    }
}

/**
 * Draws ordered layer backdrops into one effect. Adapted from BakaSU Manager's Apache-2.0
 * CombinedBackdrop approach: the source is recorded in a graphics layer and each consumer samples
 * it using live layout coordinates, so scrolling never waits for a later bitmap capture.
 */
private class CombinedBackdrop(
    private val backdrops: List<Backdrop>
) : Backdrop {
    override val isCoordinatesDependent: Boolean = backdrops.any { it.isCoordinatesDependent }
    override val offsetResidualX: Float get() = backdrops.firstOrNull()?.offsetResidualX ?: 0f
    override val offsetResidualY: Float get() = backdrops.firstOrNull()?.offsetResidualY ?: 0f

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int
    ) {
        backdrops.forEach { backdrop ->
            with(backdrop) {
                drawBackdrop(density, coordinates, layerBlock, downscaleFactor)
            }
        }
    }
}

private val LocalDynamicBackdropGraph = staticCompositionLocalOf<DynamicBackdropGraph?> { null }

/** One shared, hardware-accelerated LayerBackdrop graph for all glass surfaces. */
@Composable
internal fun DynamicBackdropHost(content: @Composable () -> Unit) {
    val background = rememberLayerBackdrop()
    val pageContent = rememberLayerBackdrop()
    val header = rememberLayerBackdrop()
    val navigation = rememberLayerBackdrop()
    val sheetFallback = MaterialTheme.colorScheme.surface
    val sheetContent = rememberLayerBackdrop {
        // Only the recorded layer receives this base; the visible sheet keeps its configured tint.
        drawRect(sheetFallback)
        drawContent()
    }
    val graph = remember(background, pageContent, header, navigation, sheetContent) {
        DynamicBackdropGraph(
            listOf(
                DynamicBackdropLayer.BACKGROUND to background,
                DynamicBackdropLayer.CONTENT to pageContent,
                DynamicBackdropLayer.HEADER to header,
                DynamicBackdropLayer.NAVIGATION to navigation,
                DynamicBackdropLayer.SHEET_CONTENT to sheetContent
            )
        )
    }
    CompositionLocalProvider(LocalDynamicBackdropGraph provides graph, content = content)
}

/** Records this layer once for all higher glass surfaces, without root-view bitmap capture. */
internal fun Modifier.dynamicBackdropSource(layer: Float): Modifier = composed {
    // miuix-blur uses Android RuntimeShader, just as BakaSU Manager does.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val backdrop = LocalDynamicBackdropGraph.current?.source(layer) ?: return@composed this
    layerBackdrop(backdrop)
}

/** Samples every lower recorded layer with BakaSU Manager's adaptive texture-blur pipeline. */
internal fun Modifier.dynamicBackdropEffect(
    blurRadius: Dp = 25.dp,
    layer: Float = DynamicBackdropLayer.CONTENT
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val graph = LocalDynamicBackdropGraph.current ?: return@composed this
    val backdrop = remember(graph, layer) { graph.below(layer) } ?: return@composed this
    drawBackdrop(
        backdrop = backdrop,
        shape = { RectangleShape },
        effects = {
            textureBlurEffect(
                blurRadiusX = blurRadius.value,
                noiseCoefficient = 0f
            )
        }
    )
}

/** A live backdrop layer; callers draw their translucent Material tint above it. */
@Composable
internal fun BoxScope.DynamicBackdropBlur(
    modifier: Modifier,
    blurRadius: Dp = 25.dp,
    layer: Float = DynamicBackdropLayer.CONTENT
) {
    Box(modifier.dynamicBackdropEffect(blurRadius, layer))
}
