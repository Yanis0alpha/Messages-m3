package com.anindra.messages.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlurEffect

/** Shared layer ordering for same-frame backdrop composition. */
internal object DynamicBackdropLayer {
    const val BACKGROUND = 0f
    // Settings pre-blurs its static expressive background once; moving cards only copy it.
    const val SETTINGS_BLURRED_BACKGROUND = 0.5f
    const val SETTINGS_CARD = 0.75f
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
    private val movingOwners = mutableStateMapOf<Any, Unit>()
    var motionFrame by mutableIntStateOf(0)
        private set

    val motionActive: Boolean get() = movingOwners.isNotEmpty()

    fun setMoving(owner: Any, moving: Boolean) {
        if (moving) movingOwners[owner] = Unit else movingOwners.remove(owner)
    }

    fun advanceMotionFrame() {
        motionFrame = if (motionFrame == Int.MAX_VALUE) 0 else motionFrame + 1
    }

    fun source(layer: Float): LayerBackdrop? = layers.firstOrNull { it.first == layer }?.second

    fun below(layer: Float): Backdrop? {
        // Chat menu pills and pre-blurred Settings cards use one coordinate space each. Avoiding
        // unrelated layers prevents a two-pane inbox or the raw background from shifting them.
        val lower = layers.asSequence()
            .filter { (sourceLayer, _) ->
                when (layer) {
                    DynamicBackdropLayer.SHEET_FLOATING_CONTENT ->
                        sourceLayer == DynamicBackdropLayer.SHEET_CONTENT
                    DynamicBackdropLayer.SETTINGS_CARD ->
                        sourceLayer == DynamicBackdropLayer.SETTINGS_BLURRED_BACKGROUND
                    else ->
                        sourceLayer < layer &&
                            sourceLayer != DynamicBackdropLayer.SETTINGS_BLURRED_BACKGROUND
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
    val settingsBlurredBackground = rememberLayerBackdrop()
    val pageContent = rememberLayerBackdrop()
    val header = rememberLayerBackdrop()
    val navigation = rememberLayerBackdrop()
    val sheetFallback = MaterialTheme.colorScheme.surface
    val sheetContent = rememberLayerBackdrop {
        // Only the recorded layer receives this base; the visible sheet keeps its configured tint.
        drawRect(sheetFallback)
        drawContent()
    }
    val graph = remember(
        background,
        settingsBlurredBackground,
        pageContent,
        header,
        navigation,
        sheetContent
    ) {
        DynamicBackdropGraph(
            listOf(
                DynamicBackdropLayer.BACKGROUND to background,
                DynamicBackdropLayer.SETTINGS_BLURRED_BACKGROUND to settingsBlurredBackground,
                DynamicBackdropLayer.CONTENT to pageContent,
                DynamicBackdropLayer.HEADER to header,
                DynamicBackdropLayer.NAVIGATION to navigation,
                DynamicBackdropLayer.SHEET_CONTENT to sheetContent
            )
        )
    }
    // Scrollers and draggable sheets are commonly translated as cached graphics layers. Refresh
    // only while one is moving so the sampled coordinates follow the current frame, then go idle.
    LaunchedEffect(graph) {
        snapshotFlow { graph.motionActive }.collectLatest { active ->
            if (!active) return@collectLatest
            while (true) {
                withFrameNanos { }
                graph.advanceMotionFrame()
            }
        }
    }
    CompositionLocalProvider(LocalDynamicBackdropGraph provides graph, content = content)
}

/** Keeps live glass aligned while a scroll or sheet offset is changing, then fully suspends. */
@Composable
internal fun DynamicBackdropMotionTracker(
    settleDelayMillis: Long = 80L,
    position: () -> Any?
) {
    val graph = LocalDynamicBackdropGraph.current
    val owner = remember(graph) { Any() }
    val currentPosition by rememberUpdatedState(position)

    LaunchedEffect(graph, owner) {
        if (graph == null) return@LaunchedEffect
        var initial = true
        snapshotFlow { currentPosition() }.collectLatest {
            // Establish the laid-out resting position without waking HWUI. Only later changes are
            // movement; this also avoids a synthetic burst for every tracker during app startup.
            if (initial) {
                initial = false
                return@collectLatest
            }
            // Keep one frame-synchronised refresh loop alive through drags and flings. The graph,
            // not each moving owner, advances the tick so concurrent motions still emit once/frame.
            graph.setMoving(owner, true)
            delay(settleDelayMillis)
            graph.setMoving(owner, false)
        }
    }
    DisposableEffect(graph, owner) {
        onDispose { graph?.setMoving(owner, false) }
    }
}

/** Records this layer once for all higher glass surfaces, without root-view bitmap capture. */
internal fun Modifier.dynamicBackdropSource(
    layer: Float,
    followMotion: Boolean = false
): Modifier = composed {
    // miuix-blur uses Android RuntimeShader, just as BakaSU Manager does.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val graph = LocalDynamicBackdropGraph.current ?: return@composed this
    val backdrop = graph.source(layer) ?: return@composed this
    val refreshed = if (followMotion) {
        this.drawWithContent {
            graph.motionFrame
            drawContent()
        }
    } else {
        this
    }
    refreshed.layerBackdrop(backdrop)
}

/** Samples every lower recorded layer with BakaSU Manager's adaptive texture-blur pipeline. */
internal fun Modifier.dynamicBackdropEffect(
    blurRadius: Dp = 25.dp,
    layer: Float = DynamicBackdropLayer.CONTENT,
    followMotion: Boolean = false
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val graph = LocalDynamicBackdropGraph.current ?: return@composed this
    val backdrop = remember(graph, layer) { graph.below(layer) } ?: return@composed this
    drawBackdrop(
        backdrop = backdrop,
        shape = { RectangleShape },
        effects = {
            // Match BakaSU: this state read makes miuix recompute its remembered consumer
            // coordinates on the current frame even when an ancestor only transforms the layer.
            if (followMotion) graph.motionFrame
            if (blurRadius.value > 0f) {
                textureBlurEffect(
                    blurRadiusX = blurRadius.value,
                    noiseCoefficient = 0f
                )
            }
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
